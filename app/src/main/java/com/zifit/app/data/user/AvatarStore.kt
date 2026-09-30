/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.user

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 头像文件的存取。**只做文件 IO，不碰配置** —— 路径记在 [UserProfile.avatarPath]。
 *
 * 做成接口是为了让 ViewModel 在 JVM 单测里能喂一个假实现（真实实现要 `Context`
 * 与 `contentResolver`，单测里没有）。
 */
interface AvatarStore {

  /**
   * 把 [uri] 指向的图片复制进应用私有目录，返回新文件的**绝对路径**；失败返回 null。
   *
   * 复制而不是直接用 URI：`ACTION_OPEN_DOCUMENT` 给的读权限在进程重启后不保证还在，
   * 用户把原图删了更是直接读不到 —— 头像会莫名其妙变空白。复制一份小的代价换稳定。
   */
  suspend fun save(uri: Uri): String?

  /** 删掉指定路径的文件（用于用户换头像后清旧图）。失败静默 —— 删不掉不值得打断用户。 */
  fun delete(path: String?)
}

/** 落盘实现：文件放在 `filesDir/avatar/` 下，随应用卸载一起消失。 */
class FileAvatarStore(private val context: Context) : AvatarStore {

  override suspend fun save(uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.filesDir, DIR_NAME)
        dir.mkdirs()
        // 文件名带时间戳：Coil 按路径做**内存缓存**，沿用同一个名字换了图也只会显示旧图
        val target = File(dir, "avatar-${System.currentTimeMillis()}.jpg")
        val opened =
          context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
            true
          } ?: false
        if (!opened) return@runCatching null
        // 旧头像没用了，顺手删掉，别让私有目录里堆一串历史图片
        dir.listFiles()?.forEach { file -> if (file != target) file.delete() }
        target.absolutePath
      }
      .getOrNull()
  }

  override fun delete(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching { File(path).delete() }
  }

  private companion object {
    const val DIR_NAME = "avatar"
  }
}
