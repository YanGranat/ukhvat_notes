package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhvat.notes.R
import com.ukhvat.notes.data.LocaleManager
import com.ukhvat.notes.domain.model.Language
import com.ukhvat.notes.ui.theme.rememberGlobalColors

/**
 * App language selection dialog in ExportOptionsDialog style
 */
@Composable
fun LanguageDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    // Context removed as it's not used in this dialog
    val colors = rememberGlobalColors()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 0.dp), // Like ThemeSelector
                verticalArrangement = Arrangement.Center
            ) {
                
                OutlinedButton(
                    onClick = {
                        onLanguageSelected(Language.RUSSIAN)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = if (currentLanguage == Language.RUSSIAN) {
                            colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = BorderStroke(
                        width = if (currentLanguage == Language.RUSSIAN) 2.dp else 1.dp,
                        color = if (currentLanguage == Language.RUSSIAN) {
                            colors.primary
                        } else {
                            Color(0xFF757575)
                        }
                    )
                ) {
                    Text(stringResource(R.string.language_russian))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        onLanguageSelected(Language.ENGLISH)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = if (currentLanguage == Language.ENGLISH) {
                            colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = BorderStroke(
                        width = if (currentLanguage == Language.ENGLISH) 2.dp else 1.dp,
                        color = if (currentLanguage == Language.ENGLISH) {
                            colors.primary
                        } else {
                            Color(0xFF757575)
                        }
                    )
                ) {
                    Text(stringResource(R.string.language_english))
                }
            }
        },
        confirmButton = {}
    )
}