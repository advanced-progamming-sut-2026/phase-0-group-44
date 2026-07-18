# Password persistence

Passwords are stored only as lowercase SHA-256 hexadecimal digests. Registration,
password recovery, profile changes, and login all use `PasswordService`; login
hashes the supplied password and compares digests.

On load, a compatible legacy `hashOfPassword` value that is non-empty and is not
already a 64-character hexadecimal digest is treated as plaintext, hashed once,
and immediately rewritten through the repository's atomic save path. Existing
digests are never hashed again. A legacy plaintext password consisting of exactly
64 hexadecimal characters is indistinguishable from a digest and is therefore
left unchanged rather than risking destructive double hashing.

No controller, result message, migration message, or save diagnostic includes a
raw password.
