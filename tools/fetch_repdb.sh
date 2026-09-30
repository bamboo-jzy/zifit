#!/usr/bin/env bash
#
# 拉取 RepDB 免费层数据集到 app/src/main/assets/repdb/。
#
# 为什么用脚本、不把数据提交进仓库：
#   RepDB Free Tier License v1.0 的 Term 3 禁止「把数据集 —— 或修改/派生的数据集 ——
#   作为数据集、数据集仓库或 API 再分发」，而本仓库是 public。
#   数据只随 APK 分发（Term 1 允许的 in-app use），故不进版本库。
#
# 许可要点（快照 2026-09-16）：
#   Term 1  个人与商业「应用内」使用免费
#   Term 2  必须署名 —— "Exercise data by RepDB (repdb.co)"
#   Term 3  禁止作为数据集再分发（In-app use only）；派生数据集同样禁止
#   Term 4  图片可缩放/裁剪/改色用于应用内
#   Term 5  图片不得作为生成式模型的输入、参考或条件材料（输出亦视为派生数据集）
#   Term 6  premium-samples/ 仅评估用，不得用于生产或再分发 —— 本脚本刻意不拉取
#
# 用法： tools/fetch_repdb.sh [--force]
set -euo pipefail

ZIP_URL="https://codeload.github.com/RepDB/exercise-dataset/zip/refs/heads/main"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/repdb"
STAMP="$DEST/SNAPSHOT.txt"

EXPECT_EXERCISES=601

if [ -f "$STAMP" ] && [ "${1:-}" != "--force" ]; then
  echo "已存在数据集快照："
  sed 's/^/  /' "$STAMP"
  echo "重新拉取请加 --force"
  exit 0
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "→ 下载 RepDB 免费层快照…"
curl -fsSL --retry 3 --max-time 600 -o "$TMP/repo.zip" "$ZIP_URL"
echo "  zip: $(stat -c%s "$TMP/repo.zip") bytes"

echo "→ 解包（仅 exercises.json + images/flat/，不含 premium-samples）"
unzip -q "$TMP/repo.zip" \
  "exercise-dataset-main/exercises.json" \
  "exercise-dataset-main/images/flat/*" \
  "exercise-dataset-main/LICENSE-DATA.md" \
  "exercise-dataset-main/ATTRIBUTION.md" \
  -d "$TMP/x"

SRC="$TMP/x/exercise-dataset-main"
rm -rf "$DEST"
mkdir -p "$DEST"
cp "$SRC/exercises.json" "$DEST/"
cp -r "$SRC/images" "$DEST/"
cp "$SRC/LICENSE-DATA.md" "$SRC/ATTRIBUTION.md" "$DEST/"

# ---- 校验：条数与图片数不符就报错，避免半个数据集悄悄进包 ----
COUNT="$(grep -oE '"count"[[:space:]]*:[[:space:]]*[0-9]+' "$DEST/exercises.json" | head -1 | grep -oE '[0-9]+$')"
IMAGES="$(find "$DEST/images/flat" -name '*.webp' | wc -l | tr -d ' ')"

if [ "$COUNT" != "$EXPECT_EXERCISES" ]; then
  echo "✗ 条数异常：期望 $EXPECT_EXERCISES，实得 ${COUNT:-0}" >&2
  exit 1
fi
if [ "$IMAGES" -lt 1000 ]; then
  echo "✗ 图片数异常：${IMAGES}（应 ≥1000）" >&2
  exit 1
fi

{
  echo "source: $ZIP_URL"
  echo "fetched: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "exercises: $COUNT"
  echo "images: $IMAGES"
  echo "license: RepDB Free Tier License v1.0 (see LICENSE-DATA.md)"
} > "$STAMP"

echo "✓ 完成：$DEST"
sed 's/^/  /' "$STAMP"
