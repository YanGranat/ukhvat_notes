package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhvat.notes.R
import com.ukhvat.notes.ui.theme.rememberGlobalColors

@Composable
fun SettingsDialog(
    onApiKeysClick: () -> Unit,
    onModelSelectionClick: () -> Unit,
    onQuickNoteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberGlobalColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 0.dp)
            ) {
                OutlinedButton(
                    onClick = onApiKeysClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.settings_api_keys))
                }

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onModelSelectionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.settings_model_selection))
                }

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onQuickNoteClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.settings_quick_note))
                }
            }
        },
        confirmButton = {}
    )
}


