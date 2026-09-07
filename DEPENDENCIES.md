# Required runtime dependencies

Target Minecraft: **1.21.1**
Target loader: **NeoForge**
Target Java: **21**

| Mod/library | Version | Why |
|---|---|---|
| Create | 6.x.x (6.0.0–<7.0.0) | Required by CEE and P&W |
| Create: Electro Energetics | 1.21.1-1.1.1 | Electrical/train system |
| Create: Pantographs & Wires | 1.21.1-beta-0.2.3-C6 | OHE/wire/pantograph system |
| DragonLib | 1.21.1-beta-3.0.28 | Required by P&W |
| GeckoLib | 1.21.1-4.8.4 | Required by P&W |

CEE declares Create 6.0.7 through <6.1.0 as its supported dependency range.
P&W C6 declares Create >=6.0.7 and requires DragonLib >=3.0.28 and GeckoLib >=4.8.4.

The build downloads DragonLib/GeckoLib from Curse Maven. The two target mod JARs are included in `libs/` for the exact API target.

Do not mix Fabric/Forge variants with this project.


**Create compatibility:** this mod now declares the full Create 6.x.x major range. The build API baseline remains 6.0.7.
