package com.hani.btapp.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hani.btapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by hanif on 2022-08-07.
 */

@Composable
fun AppTopBar(
    title: String,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    if (scaffoldState.drawerState.isClosed) {
                        scaffoldState.drawerState.open()
                    } else {
                        scaffoldState.drawerState.close()
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "menu_bar",
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        modifier = Modifier.wrapContentHeight()
    )
}

@Composable
fun MenuItem(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1.0F else 0.5F
    Text(
        text = text.uppercase(),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onPrimary,
        modifier = Modifier
            .applyIf(enabled) {
                this.clickable {
                    onClick()
                }
            }
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.primary.copy(alpha = alpha))
            .padding(16.dp)
    )
}