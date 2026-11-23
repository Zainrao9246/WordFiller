package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.VaultMediaEntity
import com.example.data.repository.DecryptedVaultMessage
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentSky
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import com.example.ui.theme.WordTilePalette

@Composable
fun VaultMessageEditorDialog(
  existing: DecryptedVaultMessage?,
  onDismiss: () -> Unit,
  onSave: (id: String?, contact: String, body: String, accentIndex: Int) -> Unit,
) {
  var contact by remember(existing?.id) { mutableStateOf(existing?.contact ?: "") }
  var body by remember(existing?.id) { mutableStateOf(existing?.body ?: "") }
  var accentIndex by remember(existing?.id) { mutableIntStateOf(existing?.accentIndex ?: 0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = {
      Text(
        text = if (existing == null) "New Private Message" else "Edit Private Message",
        color = TextPrimary,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = contact,
          onValueChange = { contact = it },
          label = { Text("Contact / Label") },
          placeholder = { Text("e.g. Private Chat, Recovery Note") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentSky,
            unfocusedBorderColor = UnsolvedTileStroke,
            focusedContainerColor = SlateSurfaceElevated,
            unfocusedContainerColor = SlateSurfaceElevated,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_message_contact_input"),
        )

        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text("Encrypted Message") },
          placeholder = { Text("Stored AES-GCM encrypted locally...") },
          minLines = 3,
          maxLines = 6,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentSky,
            unfocusedBorderColor = UnsolvedTileStroke,
            focusedContainerColor = SlateSurfaceElevated,
            unfocusedContainerColor = SlateSurfaceElevated,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_message_body_input"),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(top = 4.dp),
        ) {
          WordTilePalette.take(6).forEachIndexed { idx, color ->
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (accentIndex == idx) 2.5.dp else 0.dp,
                  color = if (accentIndex == idx) Color.White else Color.Transparent,
                  shape = CircleShape,
                )
                .clickable { accentIndex = idx },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = body.isNotBlank(),
        onClick = { onSave(existing?.id, contact, body, accentIndex) },
        colors = ButtonDefaults.buttonColors(containerColor = PillWhite, contentColor = PillNavyInk),
        modifier = Modifier.testTag("vault_message_save_button"),
      ) {
        Text("Encrypt & Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
fun ChangePinDialog(
  requireCurrentPin: Boolean,
  onDismiss: () -> Unit,
  onChangePin: (oldPin: String, newPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
) {
  var oldPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = {
      Text(
        text = if (requireCurrentPin) "Change Vault PIN" else "Set New Vault PIN",
        color = TextPrimary,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (requireCurrentPin) {
          OutlinedTextField(
            value = oldPin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) oldPin = it },
            label = { Text("Current PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("change_pin_old_input"),
          )
        } else {
          Text(
            text = "Recovery access active — enter a new 4–6 digit PIN to replace the forgotten one.",
            style = MaterialTheme.typography.bodyMedium,
            color = AccentEmerald,
          )
        }
        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
          label = { Text("New PIN (4–6 digits)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("change_pin_new_input"),
        )
        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
          label = { Text("Confirm New PIN") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("change_pin_confirm_input"),
        )
        if (!error.isNullOrBlank()) {
          Text(
            text = error!!,
            style = MaterialTheme.typography.labelMedium,
            color = AccentCoral,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            newPin.length !in 4..6 -> error = "New PIN must be 4–6 digits"
            newPin != confirmPin -> error = "New PINs do not match"
            else -> onChangePin(if (requireCurrentPin) oldPin else "", newPin) { ok, msg ->
              if (ok) onDismiss() else error = msg
            }
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PillNavyInk),
        modifier = Modifier.testTag("change_pin_save_button"),
      ) {
        Text("Update PIN", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
fun ZeroTraceDeleteMediaDialog(
  entity: VaultMediaEntity,
  onDismiss: () -> Unit,
  onConfirmWipe: (VaultMediaEntity) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = { Text("Zero-Trace Delete?", color = TextPrimary) },
    text = {
      Text(
        "The encrypted ${entity.mediaType.lowercase()} and its thumbnail will be overwritten with random data, zeroed, and erased permanently with no recoverable trace.",
        color = TextSecondary,
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirmWipe(entity) },
        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
        modifier = Modifier.testTag("confirm_zero_trace_wipe"),
      ) {
        Text("Wipe Forever", color = TextPrimary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
fun WipeAllVaultDialog(
  onDismiss: () -> Unit,
  onConfirmWipeAll: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = { Text("Wipe Entire Vault?", color = AccentCoral) },
    text = {
      Text(
        "This overwrites and permanently erases all encrypted photos, videos, audio, documents, files, messages, and resets your Vault PIN. Your Fillwords game progress stays untouched.",
        color = TextSecondary,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmWipeAll,
        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
        modifier = Modifier.testTag("confirm_wipe_all_vault"),
      ) {
        Text("Erase Everything", color = TextPrimary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
fun ChangeDestructPinDialog(
  requireCurrentPin: Boolean,
  hasCustomDestructPin: Boolean,
  onDismiss: () -> Unit,
  onSaveDestructPin: (oldPin: String, newDestructPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
  onResetDefault: (oldPin: String, onDone: (Boolean, String) -> Unit) -> Unit,
) {
  var oldPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SlateSurface,
    title = { Text("Emergency Self-Destruct PIN", color = AccentCoral) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Must be 6 hard, non-repeating digits (never colliding with 4-digit PINs). Entering it 2 consecutive times on the lock screen zero-wipes everything in the Vault.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
        if (requireCurrentPin) {
          OutlinedTextField(
            value = oldPin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) oldPin = it },
            label = { Text("Current Vault PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("destruct_pin_old_input"),
          )
        }
        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
          label = { Text("New Self-Destruct PIN (6 hard digits)") },
          placeholder = { Text("e.g. 846291") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("destruct_pin_new_input"),
        )
        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
          label = { Text("Confirm Self-Destruct PIN") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("destruct_pin_confirm_input"),
        )
        if (!error.isNullOrBlank()) {
          Text(
            text = error!!,
            style = MaterialTheme.typography.labelMedium,
            color = AccentCoral,
          )
        }
        if (hasCustomDestructPin) {
          TextButton(
            onClick = {
              onResetDefault(if (requireCurrentPin) oldPin else "") { ok, msg ->
                if (ok) onDismiss() else error = msg
              }
            },
            modifier = Modifier.testTag("destruct_pin_reset_button"),
          ) {
            Text("Reset to Default Hard Code", color = AccentSky)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val validationError = com.example.data.repository.VaultRepository.validateHardDestructPin(newPin)
          when {
            validationError != null -> error = validationError
            newPin != confirmPin -> error = "Self-Destruct PINs do not match"
            else -> onSaveDestructPin(if (requireCurrentPin) oldPin else "", newPin) { ok, msg ->
              if (ok) onDismiss() else error = msg
            }
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral, contentColor = TextPrimary),
        modifier = Modifier.testTag("destruct_pin_save_button"),
      ) {
        Text("Save Wipe PIN", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}
