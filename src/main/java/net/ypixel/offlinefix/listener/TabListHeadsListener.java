package net.ypixel.offlinefix.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import org.slf4j.Logger;

/**
 * Listens on packets sent from the backend to connected players (server -> player direction).
 *
 * <ul>
 *   <li>{@code JOIN_GAME}: force {@code onlineMode=true} (TAB heads render only when true;
 *       the client reads it from the join-game packet) and keep {@code enforcesSecureChat=false}
 *       so offline players can still chat.</li>
 *   <li>{@code PLAYER_INFO_UPDATE}: audit that each tab-list entry carries a Mojang-signed
 *       {@code textures} property; only log, never rewrite (rewriting would invalidate the
 *       signature and make the client fall back to the default skin).</li>
 * </ul>
 */
public class TabListHeadsListener extends PacketListenerAbstract {

    private final Logger logger;

    public TabListHeadsListener(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            handleJoinGame(event);
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            auditPlayerInfo(event);
        }
    }

    private void handleJoinGame(final PacketSendEvent event) {
        final WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
        if (wrapper.isOnlineMode()) {
            return; // already advertised as online-mode; leave untouched
        }
        wrapper.setOnlineMode(true);
        // Offline players cannot produce signed chat sessions; if secure chat were enforced
        // the client would reject every chat message (SignedMessageValidator.REJECT_ALL).
        wrapper.setEnforcesSecureChat(false);
        // The wrapper constructor already registered itself as the event's last-used wrapper;
        // be explicit about re-encoding so the modified fields actually go out.
        event.markForReEncode(true);
        logger.info(
            "[OfflineFix] Forced onlineMode=true on JOIN_GAME for player '{}'",
            event.getUser() != null ? event.getUser().getName() : "?"
        );
    }

    private void auditPlayerInfo(final PacketSendEvent event) {
        final WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
        if (!wrapper.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER)) {
            return;
        }
        for (final var info : wrapper.getEntries()) {
            final var profile = info.getGameProfile();
            if (profile == null) {
                continue;
            }
            final boolean hasSignedTextures = profile.getTextureProperties().stream()
                .anyMatch(p -> "textures".equals(p.getName())
                    && p.getSignature() != null
                    && !p.getSignature().isEmpty());
            if (!hasSignedTextures) {
                logger.warn(
                    "[OfflineFix] Player '{}' (UUID {}) has no Mojang-signed textures property in the tab list;"
                        + " their client will render the default skin instead.",
                    profile.getName(),
                    profile.getUUID()
                );
            }
        }
        // read-only audit: deliberately no re-encode here
    }
}
