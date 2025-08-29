package com.ukhvat.notes.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ukhvat.notes.R
import com.ukhvat.notes.ui.theme.rememberGlobalColors

/**
 * About app dialog
 * 
 * Dialog with app information in project dialog style:
 * - Slogan: "Ukhvat: simplicity outside, power inside"
 * - Design like export dialog
 * 
 * @param onDismiss Callback for dialog dismiss
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = rememberGlobalColors()
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            // Remove debug suffix from version
            packageInfo.versionName.replace("-debug", "")
        } catch (e: Exception) {
            "1"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        title = { 
            Text(
                text = stringResource(R.string.about_app),
                color = colors.dialogText
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_tagline),
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/YanGranat/ukhvat_notes"))
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.about_github), color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = colors.dialogText)
            }
        }
    )
}