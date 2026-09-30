# Combat Control

**Client-side Fabric mod for Minecraft 1.21.11**

**Repo:** https://github.com/elyaginnings-cell/combat-control

Configurable interaction delays, anchor assists, combat helpers (axe-switch, stun slam, auto-hit, mace swap). Cloth Config UI when installed.

Press **backslash** to open config.

> Warning: combat features can get you banned on PvP servers. Client-side only.

## Build

```bash
git clone https://github.com/elyaginnings-cell/combat-control.git
cd combat-control
gradle wrapper --gradle-version 9.2.0   # if wrapper jar missing
./gradlew build
```

Output: `build/libs/combat-control-*.jar`

## Layout

```
src/main/java/com/gatto/interactiondelay/
  InteractionDelay.java
  combat/        AutoHit, AxeSwitch, StunSlam, Anchors, SafeHotbar, CombatInput
  config/        Cloth + vanilla screens, JSON config
  interaction/   Category delay resolver
  mixin/         MinecraftClient, GameMenu, accessors
```

## License

MIT
