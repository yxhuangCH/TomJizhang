package com.yxhuang.jizhang.poc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    secondary = Color(0xFF4D6356),
    tertiary = Color(0xFF3D6373),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
)

@Composable
fun JizhangPocTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
