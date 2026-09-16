/*
 * Magisk Vanta — drop-in glass replacement for androidx.compose.material3.Card.
 * Mirrors Card's shape/content shape so existing call sites need only the
 * `Card(` -> `GlassCard(` rename (plus dropping `colors = CardDefaults...`,
 * replaced by the optional [tint] param below where a screen wants a
 * specific semantic color, e.g. a warning notice).
 *
 * Now supports onClick and enabled parameters for full interactivity with
 * liquid glass effects and Material ripple feedback.
 */
package com.topjohnwu.magisk.ui.glass

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    /** Overrides the default adaptive glass tint, e.g. a tertiaryContainer-style
     *  wash for a notice/warning card. [Color.Unspecified] keeps the default. */
    tint: Color = Color.Unspecified,
    /** Optional click handler. When null, the card is not clickable. */
    onClick: (() -> Unit)? = null,
    /** Whether the card is enabled. Only matters if onClick is set. */
    enabled: Boolean = true,
    /** Optional long click handler. */
    onLongClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val config = LocalGlassEffectConfig.current.let {
        if (tint.isSpecified) it.copy(surfaceTintColor = tint) else it
    }
    
    val isClickable = onClick != null && enabled
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (isClickable) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        enabled = enabled,
                        role = Role.Button,
                        indication = ripple(),
                        interactionSource = interactionSource,
                    )
                } else {
                    Modifier
                }
            )
            .liquidGlass(config = config, shape = shape),
        content = content,
    )
}
