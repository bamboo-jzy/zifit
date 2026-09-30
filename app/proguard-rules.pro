# Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
#
# This file is part of ZiFit.
# ZiFit is free software: you can redistribute it and/or modify it under the
# terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
# WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
#
# ---------------------------------------------------------------------------
# R8 / ProGuard 规则。
#
# ⚠️ 当前 `release` 构建里 `isMinifyEnabled = false`，本文件**尚未生效** ——
#    它的存在是为了让 release 变体能正常配置（AGP 会校验 proguardFiles 引用的路径）。
#    真开混淆时，下面这些"可能会踩"的位置要补规则：
#
#   - kotlinx.serialization：`@Serializable` 数据类（RepDB 的 DTO、NavKey 路由）靠生成的
#     `$$serializer` 与 `Companion.serializer()` 反射取序列化器，混淆后会找不到。
#     通常加 `-keepattributes *Annotation*, InnerClasses` 与针对 `@Serializable` 的 keep 规则。
#   - Room：`FoodDatabase` / `FoodDao` 的实现类由 KSP 生成、经反射实例化，需要 keep。
#   - Compose：`androidx.compose.*` 自带 consumer rules，一般不用手写。
#
# 顺序上建议先把 `-dontobfuscate` 打开、只做 shrink，跑通真机再逐项开混淆。
