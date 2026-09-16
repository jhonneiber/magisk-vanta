/*
 * Magisk Vanta — drop-in glass replacement for androidx.compose.material3.Card.
 * Mirrors Card's shape/content shape so existing call sites need only the
 * `Card(` -> `GlassCard(` rename (plus dropping `colors = CardDefaults...`,
 * replaced by the optional [tint] param below where a screen wants a
 * specific semantic color, e.g. a warning notice).
 */
package com.topjohnwu.magisk.ui.glass

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    /** Overrides the default adaptive glass tint, e.g. a tertiaryContainer-style
     *  wash for a notice/warning card. [Color.Unspecified] keeps the default. */
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val config = LocalGlassEffectConfig.current.let {
        if (tint.isSpecified) it.copy(surfaceTintColor = tint) else it
    }

    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier)
            .liquidGlass(
                config = config,
                shape = shape,
            ),
        content = content,
    )
}
