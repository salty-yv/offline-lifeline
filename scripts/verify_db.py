import sqlite3, os

db = r"app\src\main\assets\databases\offline_guides.db"
conn = sqlite3.connect(db)
cur = conn.cursor()

print("=== guides 表 ===")
cur.execute("SELECT id, title FROM guides ORDER BY id")
for r in cur.fetchall():
    print(f"  {r[0]:25s} {r[1]}")

print()
print("=== guide_chunks 前6行 ===")
cur.execute("SELECT chunkId, headingPath, length(body) FROM guide_chunks LIMIT 6")
for r in cur.fetchall():
    print(f"  {r[0]:20s}  {r[1]:30s}  body={r[2]}字")

print()
print("=== FTS 查询测试 ===")
cur.execute('SELECT chunkId, headingPath FROM guide_chunks_fts WHERE guide_chunks_fts MATCH ?', ('bleeding OR 出血 OR 止血',))
for r in cur.fetchall():
    print(f"  {r[0]:20s}  {r[1]}")

conn.close()
print()
print("DB size:", os.path.getsize(db), "bytes")
