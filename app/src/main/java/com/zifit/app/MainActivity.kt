/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.zifit.app.theme.ZiFitTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      ZiFitTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
