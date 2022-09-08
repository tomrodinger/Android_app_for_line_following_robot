package com.hani.btapp.ui.theme.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Created by hanif on 2022-07-25.
 */

@Composable
fun UiDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier,
        color = Color.LightGray,
        thickness = 1.dp,
    )
}

@Composable
fun VerticalSpacer(space: Dp) {
    Spacer(modifier = Modifier.height(space))
}

@Composable
fun HorizontalSpacer(space: Dp) {
    Spacer(modifier = Modifier.width(space))
}

@Composable
fun Modifier.applyIf(
    condition: Boolean,
    modifierFunction: @Composable Modifier.() -> Modifier
) = this.run {
    if (condition) {
        this.modifierFunction()
    } else {
        this
    }
}