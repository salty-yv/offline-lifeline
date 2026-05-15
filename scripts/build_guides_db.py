#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_guides_db.py
==================
读取 app/src/main/assets/guides_src/**/*.md
按 ## 二级标题切 chunk
生成 offline_guides.db（SQLite）写入 app/src/main/assets/databases/

用法（仅需 Python 标准库，无需额外安装）：
    cd scripts
    python build_guides_db.py
"""

import os
import re
import sqlite3
import time
from typing import Dict, List, Optional, Set, Tuple

# ─── 路径配置 ─────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
GUIDES_SRC_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "guides_src")
DB_OUTPUT_DIR  = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "databases")
DB_OUTPUT_PATH = os.path.join(DB_OUTPUT_DIR, "offline_guides.db")

# ─── Chunk 大小警告阈值（中文字符数）────────────────────────────────────────────
CHUNK_WARN_CHARS = 450

# ─── 全局统计 ────────────────────────────────────────────────────────────────────
warnings: List[str] = []
guide_count = 0
chunk_count = 0
topic_set: Set[str] = set()


# ─── 轻量级 YAML Front Matter 解析 ───────────────────────────────────────────────
def parse_front_matter(text: str) -> Tuple[Dict, str]:
    """
    解析 Markdown 文件开头的 YAML Front Matter（--- ... ---）。
    返回 (meta_dict, body_str)。
    仅支持简单的标量值和列表（- item），足以覆盖本项目的 Front Matter 格式。
    """
    if not text.startswith("---"):
        return {}, text

    end = text.find("\n---", 3)
    if end == -1:
        return {}, text

    yaml_block = text[3:end].strip()
    body = text[end + 4:].strip()

    meta: Dict = {}
    current_key = None
    current_list: Optional[List] = None

    for line in yaml_block.splitlines():
        # 列表项
        if line.startswith("  - ") or line.startswith("- "):
            item = line.lstrip(" -").strip()
            if current_list is not None:
                current_list.append(item)
            continue

        # key: value
        if ":" in line:
            key, _, value = line.partition(":")
            key = key.strip()
            value = value.strip()

            if value == "":
                # 下一行是列表
                current_list = []
                meta[key] = current_list
                current_key = key
            else:
                current_list = None
                current_key = key
                # 尝试转整型
                try:
                    meta[key] = int(value)
                except ValueError:
                    meta[key] = value

    return meta, body


def parse_markdown_file(filepath: str) -> Optional[Dict]:
    """解析单个 Markdown 文件，返回 guide 元数据 + chunks 列表。"""
    with open(filepath, "r", encoding="utf-8") as f:
        text = f.read()

    meta, body = parse_front_matter(text)
    body = body.strip()

    filename = os.path.basename(filepath)

    # 必须字段校验
    required = ["id", "title", "topic", "risk_domains", "tags"]
    for field in required:
        if field not in meta:
            warnings.append(f"{filename}: 缺少必须字段 '{field}'")
            return None

    if "reviewed_at" not in meta:
        warnings.append(f"{filename}: 缺少 reviewed_at")

    guide_id   = meta["id"]
    title      = meta["title"]
    topic      = meta["topic"]
    risk_domains = meta.get("risk_domains", [])
    tags         = meta.get("tags", [])
    priority     = int(meta.get("priority", 3))
    version      = int(meta.get("version", 1))
    updated_at_millis = int(time.time() * 1000)

    # summary：取正文第一个非空、非标题行
    summary = ""
    for line in body.splitlines():
        line_stripped = line.strip()
        if line_stripped and not line_stripped.startswith("#"):
            summary = line_stripped
            break

    tags_str = ",".join(tags) if isinstance(tags, list) else str(tags)

    guide_data = {
        "id":               guide_id,
        "title":            title,
        "summary":          summary,
        "body":             body,
        "tags":             tags_str,
        "updated_at_millis": updated_at_millis,
    }

    chunks = _split_into_chunks(
        body=body,
        guide_id=guide_id,
        title=title,
        topic=topic,
        risk_domains=risk_domains,
        tags_str=tags_str,
        priority=priority,
        version=version,
        updated_at_millis=updated_at_millis,
        filename=filename,
    )

    return {"guide": guide_data, "chunks": chunks, "topic": topic}


def _split_into_chunks(
    body: str,
    guide_id: str,
    title: str,
    topic: str,
    risk_domains: list,
    tags_str: str,
    priority: int,
    version: int,
    updated_at_millis: int,
    filename: str,
) -> List[Dict]:
    """按 ## 标题切 chunk。"""
    chunks = []
    sections = re.split(r"(?=^## )", body, flags=re.MULTILINE)

    chunk_index = 0
    for section in sections:
        section = section.strip()
        if not section:
            continue

        lines = section.splitlines()
        heading_line = lines[0].strip()

        if not heading_line.startswith("## "):
            continue  # 跳过文件头 # 标题

        heading = heading_line.lstrip("#").strip()
        section_body = "\n".join(lines[1:]).strip()

        if not section_body:
            continue

        if len(section_body) > CHUNK_WARN_CHARS:
            warnings.append(
                f"{filename} chunk {chunk_index + 1:03d} "
                f"超过 {CHUNK_WARN_CHARS} 字（实际 {len(section_body)} 字）"
            )

        primary_domain = risk_domains[0] if risk_domains else "UNKNOWN"
        chunk_id = f"{guide_id}_{chunk_index + 1:03d}"

        chunks.append({
            "chunk_id":        chunk_id,
            "guide_id":        guide_id,
            "topic":           topic,
            "risk_domain":     primary_domain,
            "title":           title,
            "heading_path":    f"{title} > {heading}",
            "body":            section_body,
            "tags":            tags_str,
            "priority":        priority,
            "chunk_index":     chunk_index,
            "content_version": version,
            "updated_at_millis": updated_at_millis,
        })
        chunk_index += 1

    return chunks


# ─── 建表 ────────────────────────────────────────────────────────────────────────
def create_tables(conn: sqlite3.Connection):
    cur = conn.cursor()
    cur.executescript("""
        DROP TABLE IF EXISTS guide_chunks_fts;
        DROP TABLE IF EXISTS guide_chunks;
        DROP TABLE IF EXISTS guides;

        CREATE TABLE guides (
            id               TEXT NOT NULL PRIMARY KEY,
            title            TEXT NOT NULL,
            summary          TEXT NOT NULL,
            body             TEXT NOT NULL,
            tags             TEXT NOT NULL,
            updatedAtMillis  INTEGER NOT NULL
        );

        CREATE TABLE guide_chunks (
            chunkId          TEXT NOT NULL PRIMARY KEY,
            guideId          TEXT NOT NULL,
            topic            TEXT NOT NULL,
            riskDomain       TEXT NOT NULL,
            title            TEXT NOT NULL,
            headingPath      TEXT NOT NULL,
            body             TEXT NOT NULL,
            tags             TEXT NOT NULL,
            priority         INTEGER NOT NULL,
            chunkIndex       INTEGER NOT NULL,
            contentVersion   INTEGER NOT NULL,
            updatedAtMillis  INTEGER NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_chunk_guide  ON guide_chunks(guideId);
        CREATE INDEX IF NOT EXISTS idx_chunk_topic  ON guide_chunks(topic);
        CREATE INDEX IF NOT EXISTS idx_chunk_domain ON guide_chunks(riskDomain);

        CREATE VIRTUAL TABLE guide_chunks_fts
        USING fts4(
            chunkId,
            guideId,
            topic,
            riskDomain,
            title,
            headingPath,
            body,
            tags
        );
    """)
    conn.commit()


# ─── 插入数据 ─────────────────────────────────────────────────────────────────────
def insert_guide(conn: sqlite3.Connection, g: dict):
    conn.execute(
        "INSERT OR REPLACE INTO guides VALUES (?,?,?,?,?,?)",
        (g["id"], g["title"], g["summary"], g["body"], g["tags"], g["updated_at_millis"]),
    )


def insert_chunk(conn: sqlite3.Connection, c: dict):
    conn.execute(
        "INSERT OR REPLACE INTO guide_chunks VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
        (
            c["chunk_id"], c["guide_id"], c["topic"], c["risk_domain"],
            c["title"], c["heading_path"], c["body"], c["tags"],
            c["priority"], c["chunk_index"], c["content_version"], c["updated_at_millis"],
        ),
    )
    conn.execute(
        "INSERT INTO guide_chunks_fts VALUES (?,?,?,?,?,?,?,?)",
        (
            c["chunk_id"], c["guide_id"], c["topic"], c["risk_domain"],
            c["title"], c["heading_path"], c["body"], c["tags"],
        ),
    )


# ─── 主流程 ───────────────────────────────────────────────────────────────────────
def main():
    global guide_count, chunk_count

    os.makedirs(DB_OUTPUT_DIR, exist_ok=True)
    if os.path.exists(DB_OUTPUT_PATH):
        os.remove(DB_OUTPUT_PATH)

    conn = sqlite3.connect(DB_OUTPUT_PATH)
    create_tables(conn)

    md_files: List[str] = []
    for root, _, files in os.walk(GUIDES_SRC_DIR):
        for filename in sorted(files):
            if filename.endswith(".md"):
                md_files.append(os.path.join(root, filename))

    print(f"发现 {len(md_files)} 个 Markdown 文件，开始构建...")

    for filepath in md_files:
        result = parse_markdown_file(filepath)
        if result is None:
            print(f"  [跳过] {os.path.basename(filepath)}")
            continue

        insert_guide(conn, result["guide"])
        topic_set.add(result["topic"])
        guide_count += 1

        for chunk in result["chunks"]:
            insert_chunk(conn, chunk)
            chunk_count += 1

        print(f"  [OK] {result['guide']['id']:25s}  {len(result['chunks'])} chunks")

    conn.commit()
    conn.close()

    # ── 输出报告 ──
    print()
    print("=" * 55)
    print(f"输出路径 : {DB_OUTPUT_PATH}")
    print(f"Guides   : {guide_count}")
    print(f"Chunks   : {chunk_count}")
    print(f"Topics   : {', '.join(sorted(topic_set))}")
    if warnings:
        print(f"\nWarnings ({len(warnings)}):")
        for w in warnings:
            print(f"  ! {w}")
    else:
        print("\n无警告，所有文件格式正常。")
    print("=" * 55)
    print("构建完成！")


if __name__ == "__main__":
    main()
