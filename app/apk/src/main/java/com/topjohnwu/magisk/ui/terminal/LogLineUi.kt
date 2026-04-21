package com.topjohnwu.magisk.ui.terminal

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private enum class LogSeverity { ERROR, WARN, INFO }
private val ERROR_WORDS = Regex("\\b(error|failed|failure|fatal|exception|denied|abort|invalid)\\b", RegexOption.IGNORE_CASE)
private val WARN_WORDS = Regex("\\b(warn|warning|deprecated|timeout|retry)\\b", RegexOption.IGNORE_CASE)
private val ANSI_CODES = Regex("\u001B\\[[0-9;]*m")

private fun detectSeverity(line: String): LogSeverity {
    val t = line.replace(ANSI_CODES, "")
    return when {
        t.startsWith("!") || ERROR_WORDS.containsMatchIn(t) -> LogSeverity.ERROR
        WARN_WORDS.containsMatchIn(t) -> LogSeverity.WARN
        else -> LogSeverity.INFO
    }
}

@Composable
fun StyledLogLine(
    line: String,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    val normalized = line.replace("\u0000", "").trimEnd()
    val severity = detectSeverity(normalized)
    val startsWithStepPrefix = normalized.trimStart().startsWith("-")
    if (severity == LogSeverity.INFO && startsWithStepPrefix) return

    val contentColor = when (severity) {
        LogSeverity.ERROR -> colors.error
        LogSeverity.WARN -> colors.tertiary
        LogSeverity.INFO -> {
            when {
                normalized.startsWith("*") -> colors.onSecondaryContainer
                else -> colors.onSurfaceVariant
            }
        }
    }

    Text(
        text = ansiLogText(normalized, colors),
        modifier = modifier,
        color = contentColor,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        softWrap = true
    )
}
