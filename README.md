# OfflineFix

A Velocity plugin that makes **offline-mode** clients display player avatars in the TAB list (and thus real skins) just like they would on a premium server.

How it works: the two data conditions required for "offline clients seeing premium-style TAB avatars" are patched at the Velocity network layer using PacketEvents.

## Principle (corresponds to the 26.2 client source)

The client's `PlayerTabOverlay.extractRenderState` only draws TAB avatars when `Connection.onlineMode()` is `true`, and that value is overwritten by the `onlineMode` field of every **login packet** received (`ClientboundLoginPacket` in `ClientPacketListener.handleLogin`).

Offline servers usually send `onlineMode=false`, so players get no TAB avatars. This plugin forcibly sets that field to `true` in the `JOIN_GAME` packets sent to players (and keeps/forces `enforcesSecureChat` to `false`, so offline players' chat isn't rejected with `REJECT_ALL`).

Other players' **real skins** depend on a second condition: each player-list entry carries a **Mojang-signed `textures` property** (the client verifies it locally with the embedded Mojang public key, via the `requireSecure` branch of `SkinManager.createLookup`). A backend (such as a premium Velocity network) normally forwards the full signed property as-is, so this plugin only performs a **read-only audit** on `PLAYER_INFO_UPDATE` (warnings in the log when signatures are missing) and never rewrites it — any modification would break the signature and cause a fallback to the default skin.

## Dependencies

- Requires the **PacketEvents** plugin to be installed (`packetevents-velocity`; this plugin declares its dependency by the `packetevents` id, so the load order is guaranteed automatically).
- Dependency coordinates (documentation): https://docs.packetevents.com
- Requires **Java 25** (required by `velocity-api 4.1.0`).
- Build: JDK 25 + Gradle (`/usr/bin/gradle`).
- Not required, but recommended to use together with SkinsRestorer, installing it on both the backend servers and Velocity and enabling the `modern` forwarding for best results.

## Build

```bash
cd OfflineFix
gradle build
```

Artifact: `build/libs/OfflineFix.jar` (a plain jar; PacketEvents is `provided` and not bundled).

## Installation

1. Make sure Velocity has the PacketEvents plugin (2.13.0) installed.
2. Put `OfflineFix.jar` into Velocity's `plugins/` directory.
3. Restart Velocity.

## Behavior

- Whenever a player receives a `JOIN_GAME` with the original `onlineMode=false`, the log shows:
  `[OfflineFix] Forced onlineMode=true on JOIN_GAME for player 'xxx'`
- If a player-list entry is missing the Mojang-signed textures property, the log shows:
  `[OfflineFix] Player 'xxx' (UUID ...) has no Mojang-signed textures property ... ` (the client will then display the default skin)
  (This corresponds to the "TAB avatar works, but the skin is the vanilla default" case, usually caused by the entry having no signature/no textures, or by the client being unable to reach `textures.minecraft.net` to download the skin image.)

## Porting

This plugin only handles the two server→player packets, "login packet" and "player list", and is independent of the specific backend game server; the logic can be ported to any other Netty-based proxy/server. The conclusion is the same there: **a `onlineMode=true` login packet + full signed textures passed through untouched** = the two necessary and sufficient conditions for offline clients to display premium TAB avatars.

## License

GPLv3-or-later
