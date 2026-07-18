# Canonical asset reconciliation

| Asset | Repository rows | Classification/result | SHA-256 |
|---|---:|---|---|
| `plants.csv` | 69 | 57 mandatory, 12 bonus | `538029b0f4a0fb58d88cdd2b9b14dd9dcc2e04565879094521f5421f8e42d028` |
| `zombies.csv` | 28 | 22 mandatory, 6 bonus | `404884480cbd83eab204e302f1b41bfa60b6d88b6ba1cba9bcd8acb96a52b7f3` |
| `quests.csv` | 20 | exact 20 workbook rows | `adfaf2fc0dfcd1dd2b0167b0ec4d318911aed5a98a562e3a7f1d344fbfbe11f1` |
| `adventure-levels.csv` | 16 | 4 normal, 8 special, 4 deferred boss | `ea1f8b074a18a3799abffb03c3691161a0d7bf7580c3f1e07e7a1ff802d5204c` |

## Exact comparisons

- Repository `plants.csv` is semantically row-for-row equal to the companion canonical `plants.csv`: **True**. Byte hashes differ only because of export encoding/line endings.
- Repository `quests.csv` is row-for-row equal to `quests.xlsx`: **True**.
- Canonical `quests.xlsx` SHA-256: `33d3922d706be05ace0d239275eca5610baf9b431ce578b2d05e46dda1a6e2ca`.
- Companion legacy `Quests.csv` SHA-256: `152fa00b9aead01dc06a48f29f57facce1da2c9b26b17940d8badce4dd044e54`. Its final populated quest is missing and replaced by a blank row, so the workbook is authoritative.

## Quest distribution

- Categories: `روزانه` = 14, `اصلی` = 3, `چالش (Epic)` = 3.
- Priorities: `متوسط` = 7, `بالا` = 11, `بحرانی` = 1, `کم` = 1.
- No fourth category is present in the canonical workbook; none was invented.

## Adventure structure

- Worlds: `ANCIENT_EGYPT` = 4 levels, `FROSTBITE_CAVES` = 4 levels, `BIG_WAVE_BEACH` = 4 levels, `DARK_AGES` = 4 levels.
- Kinds: `NORMAL` = 4, `SPECIAL` = 8, `BOSS` = 4.
- Special rows: 8; unique special types: 8.
  - `CONVEYOR_BELT`
  - `LOCKED_PLANTS`
  - `SAVE_OUR_SEEDS`
  - `TIMED_WAR`
  - `NIGHT_OPS`
  - `DEAD_LINE`
  - `LOVE_YOUR_PLANTS`
  - `PLANT_WHAT_YOU_GET`
