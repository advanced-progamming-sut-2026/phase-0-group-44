# Canonical plant data

`plants.csv` is the Phase 1 canonical statistics and behavior-description table.

CSV cannot preserve spreadsheet fill colors. `bonus-plants.txt` is the checked-in
classification transcribed from the cyan/blue rows in the original
`plants.xlsx` workbook supplied with the project. Every listed name is a bonus
row; every other CSV row is mandatory.

Runtime code loads both files and fails fast if the bonus list contains a name
that is not present in the CSV.
