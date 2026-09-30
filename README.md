# Combat Control

**Client-side Fabric mod for Minecraft Java Edition 1.21.11**

Configurable interaction delays, anchor assists, combat helpers (axe-switch, stun slam, auto-hit / triggerbot, mace swap), and a Cloth Config UI when Cloth Config is installed.

Press **`\\` (backslash)** to open the config menu. Falls back to a built-in tabbed screen if Cloth Config is not present.

> **Warning:** Many of the combat features (auto-hit, axe-switch, stun slam, anchor macros) are treated as cheats on PvP servers and can get you banned. Use at your own risk. Client-side only.

---

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | **1.21.11** |
| Fabric Loader | ≥ 0.18.0 |
| Java | 21 |
| Cloth Config | Optional (recommended for full UI) |
| Mod Menu | Optional |

---

## Features

### Interaction delays
Per-category right-click / use delays (not a single global FastPlace cooldown):

| Category | Default |
|----------|---------|
| Blocks, Obsidian | 2 ticks |
| Ender Pearl, Wind Charge, End Crystal | 2 ticks |
| Anchor place / Glowstone charge / Explode | configurable |
| Armor / Elytra | 4 ticks (vanilla-like) |
| Other items / block interact | Vanilla (`-1`) |

### Anchors
- **Anchor Sequence** (2.9.0-style): place → glowstone → charge → totem → boom → restore
- **Safe Anchoring**: no auto swaps; 2-tick delays on anchor kit only
- Configurable min ticks after place / on GS / on totem

### Combat
- **Auto Axe-Switch** + pre-switch + hold ticks + shield-break chance
- **Double-hit** after shield break (CPS configurable)
- **Stun Slam**: axe → mace → restore when target is blocking (needs axe + mace in hotbar)
- **Auto-Hit (triggerbot)**: crosshair + range + optional crit wait; pauses while *you* are blocking
- **Auto-Hit Mace Swap**: swap to filtered mace before hit
- **Mace filter**: 0 any · 1 Breach · 2 Density · 3 B|D · 4 B&D

### Weapons
Spear lunge / mace / sword cooldown assists, sprint reset, crit assist (optional).

---

## Build

```bash
./gradlew build
```

Output JAR: `build/libs/combat-control-*.jar`

If the Gradle wrapper JAR is missing:

```bash
gradle wrapper --gradle-version 9.2.0
./gradlew build
```

---

## Install

1. Install Fabric Loader for 1.21.11  
2. Put the mod JAR in `.minecraft/mods/`  
3. (Recommended) Install **Cloth Config** for the full settings UI  
4. Launch, press **`\\`** to configure  

Config file: `config/combatcontrol.json`

---

## Project layout

```
src/main/java/com/gatto/interactiondelay/
  InteractionDelay.java          # Client entry
  combat/                        # Axe, stun, auto-hit, anchors, hotbar
  config/                        # Cloth + vanilla screens, JSON config
  interaction/                   # Category delay resolution
  mixin/                         # MinecraftClient, GameMenu, accessors
src/main/resources/
  fabric.mod.json
  interactiondelay.mixins.json
  assets/interactiondelay/lang/
```

---

## License

MIT — see [LICENSE](LICENSE).
