# Quest source and missing-data report

## Located canonical source

The companion project archive contains `quests.xlsx` and `Quests.csv`. The
workbook is the authoritative `Quests-pvz2` equivalent because it contains 20
quest rows; the CSV export contains a blank row and omits the final
`وقت چمن‌زنی` quest. The checked-in runtime export is
`phase1/assets/Data/quests.csv`.

## Implemented from the source

- All 20 workbook rows, in workbook order.
- The three category labels actually present: `اصلی` (Main), `روزانه` (Daily),
  and `چالش (Epic)` (Epic).
- Exact condition text, reward text, priority, and variable specification.
- Machine condition/reward mappings limited to those rows.
- Daily reset only for rows explicitly categorized as Daily.

## Data not present and therefore not invented

1. The referenced document says there are four quest categories, but the
   canonical workbook names only three. No fourth category row or name exists.
2. No quest IDs exist. Runtime source identity is the exact canonical quest
   name; parameter bindings are stored only as internal instance keys.
3. No daily rotation, number-of-active-quests, activation, or random-selection
   schedule is defined. Fixed rows activate automatically; parameterized rows
   require an explicit canonical binding through `QuestService.activate`.
4. The table contains no minigame quest rows. Travel Log still exposes the
   required `minigame` page and reports the profile minigame counter.
5. Several conditions require gameplay facts not currently attributed by the
   simulation, notably killing plant/family, lawnmower kill cause, seconds since
   first wave, and the document's exact definition of board symmetry. The event
   contract supports those facts; existing publishers emit only facts the
   current domain model can prove.
6. The table contains no level-unlock reward row. The generic reward framework
   supports level transitions, but no fictional canonical quest uses one.
