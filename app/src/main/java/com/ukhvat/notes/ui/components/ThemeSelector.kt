package com.ukhvat.notes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ukhvat.notes.ui.theme.ThemePreference
import com.ukhvat.notes.ui.theme.*
import com.ukhvat.notes.ui.theme.rememberGlobalColors
import com.ukhvat.notes.R

/**
 * Button with theme contrast icon in AppBar
 */
@Composable
fun ThemeIconButton(
    currentTheme: ThemePreference,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    IconButton(
        onClick = onThemeClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_theme_contrast),
                                    contentDescription = stringResource(R.string.change_theme_desc, currentTheme.displayName),
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Simple theme selection menu in ExportOptionsDialog style
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
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
                    .padding(top = 24.dp, bottom = 0.dp), // Top 24dp, bottom 0dp
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        onThemeSelected(ThemePreference.SYSTEM)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = if (currentTheme == ThemePreference.SYSTEM) {
                            colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = BorderStroke(
                        width = if (currentTheme == ThemePreference.SYSTEM) 2.dp else 1.dp,
                        color = if (currentTheme == ThemePreference.SYSTEM) {
                            colors.primary
                        } else {
                            Color(0xFF757575)
                        }
                    )
                ) {
                                            Text(stringResource(R.string.system_theme))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        onThemeSelected(ThemePreference.LIGHT)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = if (currentTheme == ThemePreference.LIGHT) {
                            colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = BorderStroke(
                        width = if (currentTheme == ThemePreference.LIGHT) 2.dp else 1.dp,
                        color = if (currentTheme == ThemePreference.LIGHT) {
                            colors.primary
                        } else {
                            Color(0xFF757575)
                        }
                    )
                ) {
                    Text(stringResource(R.string.theme_light))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        onThemeSelected(ThemePreference.DARK)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = if (currentTheme == ThemePreference.DARK) {
                            colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = BorderStroke(
                        width = if (currentTheme == ThemePreference.DARK) 2.dp else 1.dp,
                        color = if (currentTheme == ThemePreference.DARK) {
                            colors.primary
                        } else {
                            Color(0xFF757575)
                        }
                    )
                ) {
                    Text(stringResource(R.string.theme_dark))
                }
            }
        },
        confirmButton = {}
    )
} 