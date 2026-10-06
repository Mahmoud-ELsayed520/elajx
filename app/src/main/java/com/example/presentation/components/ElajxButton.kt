package com.example.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.presentation.theme.ElajxSpacing

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    GHOST
}

/**
 * Accessible button component complying with 48.dp minimum touch target,
 * Material ripples, clear action text, and accessible loading state.
 */
@Composable
fun ElajxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "elajx_button"
) {
    val minHeight = ElajxSpacing.minTouchTarget

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = ElajxSpacing.space6, vertical = ElajxSpacing.space3)
            ) {
                ButtonContent(text = text, isLoading = isLoading)
            }
        }
        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = ElajxSpacing.space6, vertical = ElajxSpacing.space3)
            ) {
                ButtonContent(text = text, isLoading = isLoading)
            }
        }
        ButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = ElajxSpacing.space4, vertical = ElajxSpacing.space2)
            ) {
                ButtonContent(text = text, isLoading = isLoading)
            }
        }
    }
}

@Composable
private fun ButtonContent(text: String, isLoading: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
