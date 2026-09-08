package net.ypixel.offlinefix;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.ypixel.offlinefix.listener.TabListHeadsListener;
import org.slf4j.Logger;

/**
 * OfflineFix - makes an offline player's client render TAB list heads (and, when the
 * backend forwards signed textures, real skins) exactly like on an online-mode server.
 *
 * <p>Mechanism (research notes, matches the vanilla 26.2 client source):
 * <ul>
 *   <li>The player client only draws TAB heads when the last received join-game packet
 *       ({@code ClientboundLoginPacket}) carries {@code onlineMode=true}
 *       ({@code PlayerTabOverlay.extractRenderState -> Connection.onlineMode()}).</li>
 *   <li>This plugin forces {@code onlineMode=true} on every {@code JOIN_GAME} packet going
 *       from the backend to any player, and keeps {@code enforcesSecureChat=false} so that
 *       offline players (who cannot sign chat) are still able to chat.</li>
 *   <li>Other players' real skins additionally require each tab-list entry to carry an
 *       intact Mojang-signed {@code textures} property (client verifies locally with the
 *       embedded Mojang key). The backend normally forwards those unchanged, so this plugin
 *       only audits them and never rewrites them (any rewrite would break the signature).</li>
 * </ul>
 *
 * <p>Requires the {@code packetevents} plugin to be installed on the proxy (this plugin
 * compileOnly-compiles against its API, provided at runtime by that plugin).
 */
@Plugin(
    id = "offlinefix",
    name = "OfflineFix",
    version = "1.0.0",
    description = "Force onlineMode=true on the join-game packet so offline players see TAB heads like on an online-mode server.",
    authors = {"ygbs"},
    dependencies = {
        @Dependency(id = "packetevents", optional = false)
    }
)
public class OfflineFixPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private TabListHeadsListener listener;

    @Inject
    public OfflineFixPlugin(final ProxyServer proxy, final Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        try {
            this.listener = new TabListHeadsListener(logger);
            PacketEvents.getAPI().getEventManager().registerListener(listener);
            logger.info("[OfflineFix] Enabled: forcing onlineMode=true on JOIN_GAME; auditing tab-list textures");
        } catch (final Throwable t) {
            logger.error("[OfflineFix] Failed to register PacketEvents listener. Is the 'packetevents' plugin installed?", t);
        }
    }

    @Subscribe
    public void onProxyShutdown(final ProxyShutdownEvent event) {
        if (this.listener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            } catch (final Throwable ignored) {
                // proxy is shutting down anyway
            }
        }
    }
}
