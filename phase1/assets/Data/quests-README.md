# Canonical quest data

`quests.csv` is a text export of the `quests.xlsx` workbook supplied with the
project (`Quests-pvz2` equivalent asset). The workbook contains 20 quest rows.
The earlier `Quests.csv` export omitted the final `وقت چمن‌زنی` row, so the
workbook is treated as authoritative.

The runtime preserves the canonical quest name as source identity. It does not
invent numeric quest IDs. Parameterized rows require an explicit binding from a
scheduler or caller because the workbook does not define rotation or selection
rules.
