/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import android.net.Uri
import com.zifit.app.data.user.AvatarStore
import com.zifit.app.data.user.Goal
import com.zifit.app.data.user.UserProfile
import com.zifit.app.data.user.UserProfileDraftResult
import com.zifit.app.data.user.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 内存版资料仓库：真实实现依赖 `SharedPreferences`，JVM 单测里拿不到。 */
private class FakeUserProfileRepository : UserProfileRepository {
  private val state = MutableStateFlow(UserProfile())
  override val profile: StateFlow<UserProfile> = state.asStateFlow()

  override fun save(profile: UserProfile) {
    state.value = profile
  }
}

/** 假头像存储：只记录被要求删掉的路径，不碰文件系统。 */
private class FakeAvatarStore : AvatarStore {
  val deleted = mutableListOf<String>()

  override suspend fun save(uri: Uri): String? = "/fake/avatar/new.jpg"

  override fun delete(path: String?) {
    path?.let { deleted += it }
  }
}

/**
 * 个人资料 ViewModel 的逻辑：校验不过**不许落库**、换头像要清旧文件、进编辑页要重置草稿。
 *
 * 刻意不测 `pickAvatar`：它要一个真的 `Uri`，JVM 单测里拿不到（没有 Robolectric）。
 * 头像那一段的价值在「复制进私有目录」这个 IO 动作上，属于真机验证的范畴。
 *
 * 这里也**不需要** `MainDispatcherRule`：[UserProfileViewModel.save] 与 [updateDraft]
 * 都是同步函数，没走 `viewModelScope`。
 */
class UserProfileViewModelTest {

  private val repository = FakeUserProfileRepository()
  private val avatars = FakeAvatarStore()

  private fun viewModel() = UserProfileViewModel(repository, avatars)

  @Test
  fun beginEdit_seedsDraftFromSavedProfile() {
    repository.save(UserProfile(name = "JZY", birthday = "1998-06-01", age = 28))
    val vm = viewModel()

    vm.beginEdit()

    assertEquals("JZY", vm.draft.value?.name)
    assertEquals("1998-06-01", vm.draft.value?.birthday)
    assertEquals("28", vm.draft.value?.age)
  }

  /** 草稿重置必须**覆盖**上一次没保存的编辑，否则「取消后再进来」会看到阴魂不散的内容。 */
  @Test
  fun beginEdit_overwritesPreviousDraft() {
    val vm = viewModel()
    vm.beginEdit()
    vm.updateDraft { it.copy(name = "临时改的名字") }

    vm.beginEdit()

    assertEquals("", vm.draft.value?.name)
  }

  @Test
  fun save_invalidDraft_doesNotTouchRepository() {
    repository.save(UserProfile(name = "原名"))
    val vm = viewModel()
    vm.beginEdit()
    vm.updateDraft { it.copy(age = "二百岁") }

    val failure = vm.save()

    assertEquals(UserProfileDraftResult.AgeNotANumber, failure)
    assertEquals("原名", repository.profile.value.name)
    // 校验不过时草稿要留着，让用户改完再存
    assertEquals("二百岁", vm.draft.value?.age)
  }

  @Test
  fun save_validDraft_persistsAndClearsDraft() {
    val vm = viewModel()
    vm.beginEdit()
    vm.updateDraft { it.copy(name = "  自在  ", goal = Goal.MUSCLE_GAIN, age = "28") }

    assertNull(vm.save())

    assertEquals("自在", repository.profile.value.name)
    assertEquals(Goal.MUSCLE_GAIN, repository.profile.value.goal)
    assertNull(vm.draft.value)
  }

  @Test
  fun save_replacingAvatar_deletesPreviousFile() {
    repository.save(UserProfile(name = "JZY", avatarPath = "/old/avatar-1.jpg"))
    val vm = viewModel()
    vm.beginEdit()
    vm.updateDraft { it.copy(avatarPath = "/new/avatar-2.jpg") }

    assertNull(vm.save())

    assertEquals(listOf("/old/avatar-1.jpg"), avatars.deleted)
    assertEquals("/new/avatar-2.jpg", repository.profile.value.avatarPath)
  }

  /** 移除头像（草稿里路径清空）同样要删旧文件 —— 否则私有目录里会留一张再也用不到的图。 */
  @Test
  fun save_removingAvatar_deletesPreviousFile() {
    repository.save(UserProfile(name = "JZY", avatarPath = "/old/avatar-1.jpg"))
    val vm = viewModel()
    vm.beginEdit()
    vm.updateDraft { it.copy(avatarPath = "") }

    assertNull(vm.save())

    assertEquals(listOf("/old/avatar-1.jpg"), avatars.deleted)
    assertEquals("", repository.profile.value.avatarPath)
  }

  /** 头像没换时**不许**删旧文件 —— 那会把用户当前正在用的头像删掉。 */
  @Test
  fun save_keepingSameAvatar_doesNotDelete() {
    repository.save(UserProfile(name = "JZY", avatarPath = "/same/avatar.jpg"))
    val vm = viewModel()
    vm.beginEdit()

    assertNull(vm.save())

    assertTrue(avatars.deleted.isEmpty())
    assertEquals("/same/avatar.jpg", repository.profile.value.avatarPath)
  }
}
