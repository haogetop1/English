package com.xunji.flashcards

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xunji.flashcards.ui.XunjiApp
import com.xunji.flashcards.ui.components.EmptyState
import com.xunji.flashcards.ui.theme.XunjiTheme

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        setContent {
            var retry by remember { mutableIntStateOf(0) }
            val container=remember(retry) { runCatching {(application as XunjiApplication).container} }
            container.getOrNull()?.let {XunjiApp(it)} ?: XunjiTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.statusBarsPadding().padding(22.dp)) { EmptyState("暂时无法打开寻记","你的数据会保留。请重试，或稍后重新打开应用。","重试",{retry++}) } } }
        }
    }
}
