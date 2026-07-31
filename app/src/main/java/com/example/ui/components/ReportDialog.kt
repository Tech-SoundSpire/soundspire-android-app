package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val AccentOrange = Color(0xFFFA6400)
private val REASONS = listOf(
    "spam" to "Spam",
    "harassment" to "Harassment or bullying",
    "hate" to "Hate speech",
    "sexual" to "Sexual or explicit content",
    "violence" to "Violence or threats",
    "other" to "Other",
)

// Reusable report dialog: pick a reason (+ optional details), then onSubmit(reason, details).
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit,
) {
    var reason by remember { mutableStateOf("spam") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSubmit(reason, details) }) {
                TText("Submit report", color = AccentOrange, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { TText("Cancel", color = Color.Gray) }
        },
        title = { TText("Report content", color = AccentOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                REASONS.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = reason == value, onClick = { reason = value }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = reason == value,
                            onClick = { reason = value },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentOrange),
                        )
                        TText(label, color = Color.White, fontSize = 14.sp)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 2000) details = it },
                    placeholder = { TText("Details (optional)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
