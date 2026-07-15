# Adventure configuration

`adventure-chapters.csv` defines the chapter-wide terrain and wave rules.
`adventure-levels.csv` defines all sixteen Adventure level slots. Runtime code
loads these tables into immutable `AdventureLevelConfig` objects, validates four
levels per chapter, and validates that every mandatory special-level type occurs
exactly once. Boss rows are deferred Phase-2 placeholders and are never playable
in Phase 1.
