/*
 * Magisk Vanta — Liquid Glass floating tab bar.
 *
 * Wires this app's vendored com.topjohnwu.magisk.ui.glass.backdrop engine
 * (originally Kyant0/backdrop + Kyant0/AndroidLiquidGlass, Apache-2.0,
 * vendored via the Convx project) into MainScreen's bottom navigation,
 * replacing the stock Material3 ShortNavigationBar with a floating,
 * translucent, lens-refracting pill — the same visual language Apple calls
 * "Liquid Glass".
 */
package com.topjohnwu.magisk.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.glass.backdrop.Backdrop
import com.topjohnwu.magisk.ui.glass.backdrop.catalog.components.LiquidBottomTab
import com.topjohnwu.magisk.ui.glass.backdrop.catalog.components.LiquidBottomTabs

/** One entry in the floating tab bar. */
data class LiquidTabItem(
    val icon: ImageVector,
    val label: String,
)

/**
 * Floating Liquid Glass replacement for [androidx.compose.material3.ShortNavigationBar].
 * [backdrop] must be the same [Backdrop] applied via `Modifier.layerBackdrop(...)` to the
 * screen content behind this bar, so the glass can sample and blur/refract it live.
 */
@Composable
fun LiquidTabBar(
    tabs: List<LiquidTabItem>,
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        LiquidBottomTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabs.size,
            height = 64.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                LiquidBottomTab(onClick = { onTabSelected(index) }) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
