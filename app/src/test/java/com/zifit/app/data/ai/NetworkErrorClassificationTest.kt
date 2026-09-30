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
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连不上时的病因归类。
 *
 * ⚠️ 这一组之所以值得单测：几个异常**互为父子**（`UnknownHostException` /
 * `ConnectException` 都是 `SocketException` 的子类），判断顺序一改，
 * 界面上的话术就整体串档，而真机上很难同时造出这几种错。
 */
class NetworkErrorClassificationTest {

  @Test
  fun unknownHostMeansDns() {
    assertEquals(NetworkErrorKind.DNS, classifyNetworkError(UnknownHostException("example.invalid")))
  }

  @Test
  fun timeoutIsItsOwnKind() {
    assertEquals(NetworkErrorKind.TIMEOUT, classifyNetworkError(SocketTimeoutException("timeout")))
  }

  @Test
  fun connectRefusedIsRefused() {
    assertEquals(NetworkErrorKind.REFUSED, classifyNetworkError(ConnectException("ECONNREFUSED")))
  }

  @Test
  fun noRouteIsAlsoTreatedAsRefused() {
    assertEquals(NetworkErrorKind.REFUSED, classifyNetworkError(NoRouteToHostException("no route")))
  }

  @Test
  fun sslFailureIsTls() {
    assertEquals(NetworkErrorKind.TLS, classifyNetworkError(SSLException("cert")))
  }

  @Test
  fun anythingElseFallsBackToOther() {
    assertEquals(NetworkErrorKind.OTHER, classifyNetworkError(IOException("???")))
  }
}
