package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentSky
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PillNavyInk
import com.example.ui.theme.PillWhite
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnsolvedTileStroke
import kotlin.math.roundToInt

@Composable
fun PinGateOverlay(
  isSetupMode: Boolean,
  errorMessage: String?,
  errorNonce: Long,
  lockoutSeconds: Int,
  onClearError: () -> Unit,
  onSetupPin: (String) -> Unit,
  onSubmitPin: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  var enteredPin by remember(isSetupMode) { mutableStateOf("") }
  var firstStepPin by remember(isSetupMode) { mutableStateOf<String?>(null) }
  var localError by remember(isSetupMode) { mutableStateOf<String?>(null) }
  val shakeOffset = remember { Animatable(0f) }

  val effectiveError = localError ?: errorMessage
  LaunchedEffect(localError, errorMessage, errorNonce) {
    if (!effectiveError.isNullOrBlank()) {
      enteredPin = ""
      val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 0f)
      for (x in offsets) {
        shakeOffset.animateTo(x, tween(36))
      }
    }
  }

  val isLockedOut = lockoutSeconds > 0
  val headerTitle = when {
    !isSetupMode -> "Enter Vault PIN"
    firstStepPin == null -> "Create Vault PIN"
    else -> "Confirm Vault PIN"
  }
  val subtitle = when {
    !isSetupMode -> "Private encrypted photos, videos & messages"
    firstStepPin == null -> "Choose 4–6 digits to lock your private vault"
    else -> "Re-enter your PIN to confirm"
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("pin_gate_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    val isCompactHeight = maxHeight < 620.dp
    val keySize = if (isCompactHeight) 52.dp else 68.dp
    val rowGap = if (isCompactHeight) 8.dp else 12.dp
    val sectionGap = if (isCompactHeight) 10.dp else 18.dp
    val lockBadgeSize = if (isCompactHeight) 46.dp else 64.dp

    IconButton(
      onClick = onDismiss,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 8.dp, end = 12.dp)
        .minimumInteractiveComponentSize()
        .testTag("pin_cancel_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = "Close PIN screen",
        tint = TextSecondary,
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 400.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = if (isCompactHeight) 8.dp else 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
      Box(
        modifier = Modifier
          .size(lockBadgeSize)
          .clip(CircleShape)
          .background(AccentSky.copy(alpha = 0.16f))
          .border(1.5.dp, AccentSky.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = null,
          tint = AccentSky,
          modifier = Modifier.size(if (isCompactHeight) 22.dp else 30.dp),
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = headerTitle,
          style = if (isCompactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
          textAlign = TextAlign.Center,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          textAlign = TextAlign.Center,
        )
      }

      // PIN dot indicators
      Row(
        modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val slots = maxOf(4, enteredPin.length)
        for (i in 0 until slots.coerceAtMost(6)) {
          val filled = i < enteredPin.length
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(if (filled) AccentEmerald else SlateSurfaceElevated)
              .border(
                width = 1.5.dp,
                color = if (filled) AccentEmerald else UnsolvedTileStroke,
                shape = CircleShape,
              ),
          )
        }
      }

      if (!effectiveError.isNullOrBlank()) {
        Text(
          text = effectiveError,
          style = MaterialTheme.typography.labelMedium,
          color = AccentCoral,
          textAlign = TextAlign.Center,
        )
      }

      // Numeric Keypad
      val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
        rows.forEach { rowDigits ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            rowDigits.forEach { digit ->
              PinKeyButton(
                label = digit,
                keySize = keySize,
                enabled = !isLockedOut && enteredPin.length < 6,
                onClick = {
                  localError = null
                  onClearError()
                  enteredPin += digit
                },
                modifier = Modifier.testTag("pin_digit_$digit"),
              )
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = SlateSurface,
            shape = CircleShape,
            modifier = Modifier
              .size(keySize)
              .clip(CircleShape)
              .clickable(enabled = !isLockedOut && enteredPin.isNotEmpty()) {
                localError = null
                onClearError()
                enteredPin = enteredPin.dropLast(1)
              }
              .testTag("pin_backspace_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = if (enteredPin.isNotEmpty()) TextPrimary else TextMuted,
              )
            }
          }

          PinKeyButton(
            label = "0",
            keySize = keySize,
            enabled = !isLockedOut && enteredPin.length < 6,
            onClick = {
              localError = null
              onClearError()
              enteredPin += "0"
            },
            modifier = Modifier.testTag("pin_digit_0"),
          )

          Surface(
            color = if (enteredPin.length >= 4 && !isLockedOut) AccentEmerald else SlateSurface,
            shape = CircleShape,
            modifier = Modifier
              .size(keySize)
              .clip(CircleShape)
              .clickable(enabled = enteredPin.length >= 4 && !isLockedOut) {
                if (!isSetupMode) {
                  onSubmitPin(enteredPin)
                } else if (firstStepPin == null) {
                  firstStepPin = enteredPin
                  enteredPin = ""
                  localError = null
                } else if (firstStepPin == enteredPin) {
                  onSetupPin(enteredPin)
                } else {
                  firstStepPin = null
                  enteredPin = ""
                  localError = "PINs did not match — try again"
                }
              }
              .testTag("pin_submit_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Confirm PIN",
                tint = if (enteredPin.length >= 4 && !isLockedOut) PillNavyInk else TextMuted,
              )
            }
          }
        }
      }

      if (isSetupMode && firstStepPin != null) {
        TextButton(
          onClick = {
            firstStepPin = null
            enteredPin = ""
            localError = null
          },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("Start Over", color = TextSecondary)
        }
      } else {
        Spacer(Modifier.height(2.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = PillWhite.copy(alpha = 0.1f), contentColor = TextSecondary),
          shape = RoundedCornerShape(50),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("Return to Fillwords")
        }
      }
    }
  }
}

@Composable
private fun PinKeyButton(
  label: String,
  keySize: Dp,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = SlateSurfaceElevated,
    shape = CircleShape,
    modifier = modifier
      .size(keySize)
      .clip(CircleShape)
      .border(1.dp, UnsolvedTileStroke.copy(alpha = 0.45f), CircleShape)
      .clickable(enabled = enabled, onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (keySize < 60.dp) 20.sp else 24.sp,
        color = if (enabled) TextPrimary else TextMuted,
      )
    }
  }
}
