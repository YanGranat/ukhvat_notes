package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.AiProvider
import com.ukhvat.notes.ui.theme.rememberGlobalColors

@Composable
fun ModelSelectionDialog(
    currentProvider: AiProvider,
    currentOpenAiModel: String?,
    currentGeminiModel: String?,
    currentAnthropicModel: String?,
    currentOpenRouterModel: String?,
    onSave: (provider: AiProvider, openAi: String?, gemini: String?, anthropic: String?, openRouter: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberGlobalColors()
    var provider by remember { mutableStateOf(currentProvider) }
    var openAiModel by remember { mutableStateOf(currentOpenAiModel ?: "gpt-5-2025-08-07") }
    var geminiModel by remember { mutableStateOf(currentGeminiModel ?: "gemini-2.5-flash") }
    var anthropicModel by remember { mutableStateOf(currentAnthropicModel ?: "claude-3-7-sonnet-thinking") }
    var openRouterModel by remember { mutableStateOf(currentOpenRouterModel ?: "deepseek/deepseek-chat-v3-0324:free") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 0.dp)
            ) {
                // Provider buttons (full-width, stacked)
                Text(stringResource(R.string.provider), color = colors.dialogText)
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "OpenAI",
                    selected = provider == AiProvider.OPENAI,
                    onClick = { provider = AiProvider.OPENAI }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "Gemini",
                    selected = provider == AiProvider.GEMINI,
                    onClick = { provider = AiProvider.GEMINI }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "Claude",
                    selected = provider == AiProvider.ANTHROPIC,
                    onClick = { provider = AiProvider.ANTHROPIC }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "OpenRouter",
                    selected = provider == AiProvider.OPENROUTER,
                    onClick = { provider = AiProvider.OPENROUTER }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OpenAI models
                Text(stringResource(R.string.openai_models), color = colors.dialogText)
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "gpt-5-2025-08-07",
                    selected = openAiModel == "gpt-5-2025-08-07",
                    onClick = { openAiModel = "gpt-5-2025-08-07" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "gpt-5-mini-2025-08-07",
                    selected = openAiModel == "gpt-5-mini-2025-08-07",
                    onClick = { openAiModel = "gpt-5-mini-2025-08-07" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "gpt-5-nano-2025-08-07",
                    selected = openAiModel == "gpt-5-nano-2025-08-07",
                    onClick = { openAiModel = "gpt-5-nano-2025-08-07" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gemini models
                Text(stringResource(R.string.google_gemini_models), color = colors.dialogText)
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "gemini-2.5-pro",
                    selected = geminiModel == "gemini-2.5-pro",
                    onClick = { geminiModel = "gemini-2.5-pro" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "gemini-2.5-flash",
                    selected = geminiModel == "gemini-2.5-flash",
                    onClick = { geminiModel = "gemini-2.5-flash" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Anthropic models
                Text(stringResource(R.string.anthropic_models), color = colors.dialogText)
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "claude-sonnet-4-0",
                    selected = anthropicModel == "claude-sonnet-4-0",
                    onClick = { anthropicModel = "claude-sonnet-4-0" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "claude-3-7-sonnet-thinking",
                    selected = anthropicModel == "claude-3-7-sonnet-thinking",
                    onClick = { anthropicModel = "claude-3-7-sonnet-thinking" }
                )

                Spacer(modifier = Modifier.height(12.dp))
                // OpenRouter popular free models
                Text(stringResource(id = R.string.openrouter_models), color = colors.dialogText)
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "deepseek/deepseek-chat-v3-0324:free",
                    selected = openRouterModel == "deepseek/deepseek-chat-v3-0324:free",
                    onClick = { openRouterModel = "deepseek/deepseek-chat-v3-0324:free" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FullWidthSelectButton(
                    label = "google/gemma-3-27b-it:free",
                    selected = openRouterModel == "google/gemma-3-27b-it:free",
                    onClick = { openRouterModel = "google/gemma-3-27b-it:free" }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(provider, openAiModel, geminiModel, anthropicModel, openRouterModel) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FullWidthSelectButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = rememberGlobalColors()
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.dialogText,
            containerColor = if (selected) colors.primary.copy(alpha = 0.1f) else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else Color(0xFF757575)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label)
    }
}


