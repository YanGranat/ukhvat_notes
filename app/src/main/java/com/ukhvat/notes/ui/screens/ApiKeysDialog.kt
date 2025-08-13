package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhvat.notes.R
import com.ukhvat.notes.ui.theme.rememberGlobalColors

@Composable
fun ApiKeysDialog(
    openAiKeyInitial: String?,
    geminiKeyInitial: String?,
    anthropicKeyInitial: String?,
    openRouterKeyInitial: String?,
    onSave: (openAi: String, gemini: String, anthropic: String, openRouter: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberGlobalColors()
    var openAi by remember { mutableStateOf(openAiKeyInitial ?: "") }
    var gemini by remember { mutableStateOf(geminiKeyInitial ?: "") }
    var anthropic by remember { mutableStateOf(anthropicKeyInitial ?: "") }
    var openrouter by remember { mutableStateOf(openRouterKeyInitial ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 0.dp)
            ) {
                TextField(
                    value = openAi,
                    onValueChange = { openAi = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_openai)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = gemini,
                    onValueChange = { gemini = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_gemini)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = anthropic,
                    onValueChange = { anthropic = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_anthropic)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = openrouter,
                    onValueChange = { openrouter = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_openrouter)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(openAi.trim(), gemini.trim(), anthropic.trim(), openrouter.trim())
            }) {
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


