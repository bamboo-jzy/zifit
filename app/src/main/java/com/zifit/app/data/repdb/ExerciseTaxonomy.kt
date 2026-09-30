/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.repdb

/**
 * RepDB 分类枚举 → 中文标签。
 *
 * 这里译的是**解剖与训练领域的通用术语**（胸大肌、壶铃、复合动作），不是 RepDB 的
 * 创作性文本 —— 故可与源码同库；而描述/步骤/要点等原文的译文属派生内容，
 * 一律放 `assets/repdb/zh/` 且不入版本库（许可 Term 3）。
 *
 * 未收录的 key 回退为「下划线换空格」的可读形式，而不是抛错 ——
 * 数据集升级新增枚举值时不至于让界面露出空白。
 */
internal object ExerciseTaxonomy {

  private val categoryLabels =
    mapOf(
      "strength" to "力量",
      "stretching" to "拉伸",
      "cardio" to "有氧",
      "olympic" to "举重",
      "plyometrics" to "爆发力",
    )

  private val forceTypeLabels =
    mapOf(
      "push" to "推",
      "pull" to "拉",
      "static" to "静力",
      "dynamic" to "动态",
    )

  private val mechanicLabels =
    mapOf(
      "compound" to "复合动作",
      "isolation" to "孤立动作",
    )

  private val difficultyLabels =
    mapOf(
      "beginner" to "入门",
      "intermediate" to "进阶",
      "advanced" to "高阶",
    )

  private val bodyPartLabels =
    mapOf(
      "chest" to "胸部",
      "back" to "背部",
      "shoulders" to "肩部",
      "upper_arms" to "上臂",
      "lower_arms" to "前臂",
      "core" to "核心",
      "upper_legs" to "大腿",
      "lower_legs" to "小腿",
      "full_body" to "全身",
    )

  private val equipmentLabels =
    mapOf(
      // 自由重量
      "barbell" to "杠铃",
      "ez_bar" to "曲杆",
      "trap_bar" to "六角杠",
      "dumbbell" to "哑铃",
      "kettlebell" to "壶铃",
      "plates" to "杠铃片",
      // 固定器械
      "smith_machine" to "史密斯机",
      "cable" to "龙门架",
      "leg_press" to "腿举机",
      "hack_squat" to "哈克深蹲机",
      "leg_curl" to "腿弯举机",
      "leg_extension" to "腿屈伸机",
      "hip_thrust_machine" to "臀推机",
      "hip_abduction_machine" to "髋外展机",
      "hip_adduction_machine" to "髋内收机",
      "glute_ham_developer" to "臀腿训练器",
      "chest_press_machine" to "推胸机",
      "chest_fly_machine" to "夹胸机",
      "pec_deck" to "蝴蝶机",
      "shoulder_press_machine" to "肩推机",
      "plate_loaded_lateral_raise_machine" to "片式侧平举机",
      "lat_pulldown_machine" to "高位下拉机",
      "bicep_curl_machine" to "二头弯举机",
      "preacher_curl_machine" to "牧师凳弯举机",
      "tricep_extension_machine" to "三头臂屈伸机",
      "dip_machine" to "双杠臂屈伸机",
      "assisted_pullup_machine" to "助力引体机",
      "back_extension_machine" to "山羊挺身机",
      "ab_crunch_machine" to "卷腹机",
      "shrug_machine" to "耸肩机",
      "standing_calf_raise_machine" to "站姿提踵机",
      "seated_calf_raise_machine" to "坐姿提踵机",
      "donkey_calf_raise_machine" to "驴式提踵机",
      // 自重与小器械
      "pull_up_bar" to "单杠",
      "dip_station" to "双杠",
      "rings" to "吊环",
      "flat_bench" to "卧推凳",
      "stability_ball" to "瑞士球",
      "suspension_trainer" to "悬吊训练器",
      "ab_wheel" to "健腹轮",
      "plyo_box" to "跳箱",
      "loop_band" to "环状弹力带",
      "resistance_band" to "弹力带",
      "battle_rope" to "战绳",
      "climbing_rope" to "攀爬绳",
      "slam_ball" to "药球",
      "wrist_roller" to "腕力器",
      "jump_rope" to "跳绳",
      "sled" to "阻力橇",
      // 有氧器械
      "treadmill" to "跑步机",
      "stationary_bike" to "健身车",
      "air_bike" to "风阻单车",
      "elliptical" to "椭圆机",
      "rower" to "划船机",
      "stair_climber" to "爬楼机",
    )

  private val muscleLabels =
    mapOf(
      "pectoralis_major" to "胸大肌",
      "latissimus_dorsi" to "背阔肌",
      "trapezius" to "斜方肌",
      "rhomboids" to "菱形肌",
      "erector_spinae" to "竖脊肌",
      "serratus_anterior" to "前锯肌",
      "supraspinatus" to "冈上肌",
      "anterior_deltoid" to "三角肌前束",
      "lateral_deltoid" to "三角肌中束",
      "posterior_deltoid" to "三角肌后束",
      "biceps_brachii" to "肱二头肌",
      "triceps_brachii" to "肱三头肌",
      "brachialis" to "肱肌",
      "brachioradialis" to "肱桡肌",
      "forearms" to "前臂",
      "forearm_flexors" to "前臂屈肌",
      "forearm_extensors" to "前臂伸肌",
      "rectus_abdominis" to "腹直肌",
      "transverse_abdominis" to "腹横肌",
      "obliques" to "腹斜肌",
      "quadratus_lumborum" to "腰方肌",
      "gluteus_maximus" to "臀大肌",
      "gluteus_medius" to "臀中肌",
      "quadriceps" to "股四头肌",
      "hamstrings" to "腘绳肌",
      "adductors" to "内收肌",
      "abductors" to "外展肌",
      "hip_flexors" to "髋屈肌",
      "gastrocnemius" to "腓肠肌",
      "soleus" to "比目鱼肌",
    )

  private val goalLabels =
    mapOf(
      "hypertrophy" to "增肌",
      "strength" to "力量",
      "power" to "爆发力",
      "endurance" to "耐力",
      "mobility" to "灵活性",
      "core" to "核心",
      "rehabilitation" to "康复",
    )

  private val tagLabels =
    mapOf(
      // 分日标签
      "push_day" to "推日",
      "pull_day" to "拉日",
      "leg_day" to "练腿日",
      "arm_day" to "手臂日",
      "back_day" to "背部日",
      // 安全与适用性
      "knee_safe" to "膝友好",
      "lower_back_safe" to "腰友好",
      "shoulder_safe" to "肩友好",
      "no_axial_load" to "无脊柱负荷",
      "shoulder_stability" to "肩部稳定",
      "rehab" to "康复",
      // 训练风格
      "calisthenics" to "街头健身",
      "bodyweight" to "自重",
      "powerlifting" to "力量举",
      "big_three" to "三大项",
      "ballistic" to "弹震式",
      "conditioning" to "体能",
      "cardio" to "有氧",
      "warm_up" to "热身",
      // 目标部位强化
      "core" to "核心",
      "core_focus" to "核心强化",
      "glute_focus" to "臀部强化",
      "chest_focus" to "胸部强化",
      "back_focus" to "背部强化",
      "shoulder_focus" to "肩部强化",
      "calf_focus" to "小腿强化",
      "grip_focus" to "握力强化",
      "full_body" to "全身",
      "mobility" to "灵活性",
      "stretching" to "拉伸",
      // 器械条件
      "requires_bench" to "需卧推凳",
    )

  /** 全部标签共用的查表逻辑：命中返回中文，未命中回退成可读英文。 */
  private fun lookup(table: Map<String, String>, key: String?): String? =
    key?.let { table[it] ?: it.replace('_', ' ') }

  fun category(key: String?): String = lookup(categoryLabels, key).orEmpty()

  fun forceType(key: String?): String? = lookup(forceTypeLabels, key)

  fun mechanic(key: String?): String? = lookup(mechanicLabels, key)

  fun difficulty(key: String?): String = lookup(difficultyLabels, key).orEmpty()

  /**
   * 难度排序权重，越小越靠前 —— [difficultyLabels] 的**声明序即由易到难**（入门 / 进阶 / 高阶）。
   *
   * 入参是 [difficulty] 已产出的中文标签（列表里存的就是标签，不是原始 key）。
   * 顺序以词表声明序为准，**不要改成对中文 `sorted()`**：那排的是 Unicode 码位，
   * 当前这组词恰好与码位序一致纯属巧合，换一组（如 新手 / 熟练 / 专家）立刻会乱。
   * 词表外的未知值排最后。
   */
  fun difficultyRank(label: String?): Int =
    difficultyLabels.values.indexOf(label).takeIf { it >= 0 } ?: Int.MAX_VALUE

  fun bodyPart(key: String?): String = lookup(bodyPartLabels, key).orEmpty()

  /**
   * 筛选栏的分类词表在 [ExerciseCategory] —— 它是**合并与补全过**的一套
   * （腿部 = 大腿 + 小腿、臀部/有氧由肌群与 category 推出），与这里的原样部位标签不是同一套，
   * 因此不要在两者之间做字符串转换。
   */

  /** equipment 为 null 表示该动作不需要器械。 */
  fun equipment(key: String?): String = lookup(equipmentLabels, key) ?: "徒手"

  fun muscle(key: String): String = muscleLabels[key] ?: key.replace('_', ' ')

  fun muscles(keys: List<String>): List<String> = keys.map(::muscle)

  fun goals(keys: List<String>): List<String> = keys.map { goalLabels[it] ?: it.replace('_', ' ') }

  fun tags(keys: List<String>): List<String> = keys.map { tagLabels[it] ?: it.replace('_', ' ') }
}
