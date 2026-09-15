/*
 * Magisk Vanta — drop-in glass replacement for androidx.compose.material3.Card.
 * Mirrors Card's shape/content shape so existing call sites need only the
 * `Card(` -> `GlassCard(` rename (plus dropping `colors = CardDefaults...`,
 * replaced by the optional [tint] param below where a screen wants a
 * specific semantic color, e.g. a warning notice).
 */
package com.topjohnwu.magisk.ui.glass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    /** Overrides the default adaptive glass tint, e.g. a tertiaryContainer-style
     *  wash for a notice/warning card. [Color.Unspecified] keeps the default. */
    tint: Color = Color.Unspecified,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val config = LocalGlassEffectConfig.current.let {
        if (tint.isSpecified) it.copy(surfaceTintColor = tint) else it
    }
    Column(
        modifier = modifier
            .clip(shape)
            .liquidGlass(config = config, shape = shape),
        content = content,
    )
}
