package com.xunji.flashcards.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xunji.core.model.ThemeMode
private val Light=lightColorScheme(primary=Forest,onPrimary=Color.White,primaryContainer=Color(0xFFE2EDE4),onPrimaryContainer=Forest,secondary=Amber,onSecondary=Color.White,secondaryContainer=Color(0xFFF4E7CF),onSecondaryContainer=Amber,background=WarmWhite,onBackground=Ink,surface=Color.White,onSurface=Ink,surfaceVariant=Color(0xFFEEF0E9),onSurfaceVariant=Muted,outline=Color(0xFF718077),outlineVariant=Color(0xFFD8DFD6),error=Color(0xFFA33232),surfaceContainer=Color(0xFFEFEFE6),surfaceContainerLow=Color(0xFFFAF9F4))
private val Dark=darkColorScheme(primary=Color(0xFFA4D3BC),onPrimary=Color(0xFF123B2D),primaryContainer=Color(0xFF264F40),onPrimaryContainer=Color(0xFFD5EBDD),secondary=Color(0xFFE9BF7E),onSecondary=Color(0xFF3E2E14),secondaryContainer=Color(0xFF493922),onSecondaryContainer=Color(0xFFF7D9A7),background=Color(0xFF121C17),onBackground=Color(0xFFE5EDE4),surface=Color(0xFF1D2A23),onSurface=Color(0xFFE5EDE4),surfaceVariant=Color(0xFF2A3930),onSurfaceVariant=Color(0xFFB8C7BC),outline=Color(0xFF839B8B),outlineVariant=Color(0xFF3A4B40),error=Color(0xFFFFB4AB),surfaceContainer=Color(0xFF213027),surfaceContainerLow=Color(0xFF17231C))
@Composable fun XunjiTheme(mode:ThemeMode=ThemeMode.SYSTEM,content:@Composable ()->Unit) {
 val dark=when(mode) { ThemeMode.SYSTEM->isSystemInDarkTheme();ThemeMode.DARK->true;ThemeMode.LIGHT->false }
 MaterialTheme(colorScheme=if(dark)Dark else Light,typography=XunjiTypography,content=content)
}
