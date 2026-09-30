/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zifit.app.data.user.AvatarStore
import com.zifit.app.data.user.FileAvatarStore
import com.zifit.app.data.user.UserProfile
import com.zifit.app.data.user.UserProfileDraft
import com.zifit.app.data.user.UserProfileDraftResult
import com.zifit.app.data.user.UserProfileRepository
import com.zifit.app.data.user.toDraft
import com.zifit.app.data.user.toProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 个人资料的 ViewModel。
 *
 * 与 [ProfileViewModel]（管 AI 接入）**分开**：一份是「我是谁」，一份是「我连哪个模型」，
 * 两者除了都挂在「我的」下面之外没有关系，混在一个 ViewModel 里只会让它变成杂物抽屉。
 *
 * 草稿放在这里而不是页面里 `remember`：选头像要走系统图库、回来得晚，
 * 期间 Activity 可能被回收，草稿放在 ViewModel 才活得下来。
 */
internal class UserProfileViewModel(
  private val repository: UserProfileRepository,
  private val avatars: AvatarStore,
) : ViewModel() {

  val profile: StateFlow<UserProfile> = repository.profile

  /** 编辑页的草稿。null = 没在编辑。 */
  private val _draft = MutableStateFlow<UserProfileDraft?>(null)
  val draft: StateFlow<UserProfileDraft?> = _draft.asStateFlow()

  /** 进入编辑页时调用：用**已保存的**资料重置草稿。 */
  fun beginEdit() {
    _draft.value = repository.profile.value.toDraft()
  }

  fun updateDraft(transform: (UserProfileDraft) -> UserProfileDraft) {
    val current = _draft.value ?: repository.profile.value.toDraft()
    _draft.value = transform(current)
  }

  /**
   * 选好图片后立刻复制进私有目录，草稿里只记路径。
   * 异步：图库返回的图可能几 MB，不在主线程读；复制失败就什么也不做（保持原头像）。
   */
  fun pickAvatar(uri: Uri) {
    viewModelScope.launch {
      val path = avatars.save(uri) ?: return@launch
      updateDraft { it.copy(avatarPath = path) }
    }
  }

  /**
   * 保存草稿。返回 null 表示已保存，否则是**校验错误**（界面据此报「哪一栏填错了」）。
   */
  fun save(): UserProfileDraftResult? {
    val current = _draft.value ?: return null
    val result = current.toProfile()
    if (result !is UserProfileDraftResult.Valid) return result
    // 换过头像才需要删旧的：路径不同就说明上一张已经没用了
    val previous = repository.profile.value.avatarPath
    if (previous.isNotBlank() && previous != result.profile.avatarPath) {
      avatars.delete(previous)
    }
    repository.save(result.profile)
    _draft.value = null
    return null
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        // 统一取 applicationContext：ViewModel 活得比页面久，别攥着 Activity
        val app = context.applicationContext
        UserProfileViewModel(
          repository = UserProfileRepository.get(app),
          avatars = FileAvatarStore(app),
        )
      }
    }
  }
}
