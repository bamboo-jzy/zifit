/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 连不上时的**病因分类**。界面据此说人话，而不是把 Java 异常原文甩给用户。 */
enum class NetworkErrorKind {
  /** 域名解析不了 —— 地址写错，或本机 DNS 有问题。 */
  DNS,

  /** 连上了但超时。 */
  TIMEOUT,

  /** 目标端口拒绝连接 —— 地址/端口不对，或服务没起。 */
  REFUSED,

  /** HTTPS 证书校验失败（自签证书、代理中间人……）。 */
  TLS,

  /** 其余（断网、路由不可达、被系统拦……）。 */
  OTHER,
}

/**
 * 把 [IOException] 归到 [NetworkErrorKind]。
 *
 * 纯函数，可单测。⚠️ 判断顺序有讲究：`UnknownHostException` 与
 * `ConnectException` 都是 `SocketException` 的子类，**先判具体子类**，
 * 否则一律会落进父类的分支里。
 */
internal fun classifyNetworkError(e: IOException): NetworkErrorKind =
  when (e) {
    is UnknownHostException -> NetworkErrorKind.DNS
    is SocketTimeoutException -> NetworkErrorKind.TIMEOUT
    is ConnectException -> NetworkErrorKind.REFUSED
    is NoRouteToHostException -> NetworkErrorKind.REFUSED
    is SSLException -> NetworkErrorKind.TLS
    else -> NetworkErrorKind.OTHER
  }

/** 一次连通性测试的结果。**每一档都要能被界面如实翻译成人话**，不许混成一个"失败"。 */
sealed interface AiTestResult {
  /** 服务可达且鉴权通过。 */
  data class Ok(val code: Int) : AiTestResult

  /** 服务可达但密钥不被接受（401 / 403）。 */
  data class Unauthorized(val code: Int) : AiTestResult

  /**
   * 服务可达，但没有 `GET /models` 这个接口（404）。
   * ⚠️ 这**不代表密钥有问题** —— 有些代理只暴露 `/chat/completions`。
   */
  data class NoModelsEndpoint(val code: Int) : AiTestResult

  /** 其它非 2xx。 */
  data class HttpError(val code: Int) : AiTestResult

  /** 压根没连上。`kind` 决定界面上的话术，`detail` 只在 [NetworkErrorKind.OTHER] 时兜底展示。 */
  data class NetworkError(val kind: NetworkErrorKind, val detail: String) : AiTestResult
}

/**
 * 从设备上真打一次请求，判断「地址 + 密钥」能不能用。
 *
 * 做成接口是为了让 `ProfileViewModel` 可测（真实现要联网，单测里给假实现）。
 */
interface AiConnectionTester {
  suspend fun test(config: AiConfig): AiTestResult
}

/**
 * 真实实现：`GET {baseUrl}/models` 带 `Authorization: Bearer <key>`。
 *
 * 这是 OpenAI 兼容协议下**最轻**的鉴权探针（不消耗 token），DeepSeek、
 * 阿里云百炼的兼容端点都支持。
 *
 * 刻意**不做**「发一条 chat 请求试试」：那要烧 token，而且模型名写错时的报错
 * 与配置错混在一起，反而更难定位。
 *
 * 用 `HttpURLConnection` 而不是引 OkHttp/Retrofit：这里一个 GET，标准库足够；
 * AGENTS.md C3 要求依赖面越小越好。
 */
class HttpAiConnectionTester : AiConnectionTester {

  override suspend fun test(config: AiConfig): AiTestResult =
    withContext(Dispatchers.IO) {
      val connection =
        try {
          URL(config.endpoint("/models")).openConnection() as HttpURLConnection
        } catch (e: Exception) {
          // URL 拼不出来（非法字符、协议错误……）在用户侧同样是「连不上」，
          // 归到 OTHER，把原始信息带上（这类错没有更细的分类可言）
          return@withContext AiTestResult.NetworkError(
            NetworkErrorKind.OTHER,
            e.message ?: e::class.java.simpleName,
          )
        }

      try {
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        // 密钥只在这里出现一次，且只发给用户自己填的地址
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        connection.setRequestProperty("Accept", "application/json")

        when (val code = connection.responseCode) {
          in 200..299 -> AiTestResult.Ok(code)
          401, 403 -> AiTestResult.Unauthorized(code)
          404 -> AiTestResult.NoModelsEndpoint(code)
          else -> AiTestResult.HttpError(code)
        }
      } catch (e: IOException) {
        AiTestResult.NetworkError(
          kind = classifyNetworkError(e),
          detail = e.message ?: e::class.java.simpleName,
        )
      } finally {
        connection.disconnect()
      }
    }

  private companion object {
    /** 10 秒：手机网络下够用，又不至于让界面上的「正在测试…」转到让人以为卡死。 */
    const val TIMEOUT_MS = 10_000
  }
}
