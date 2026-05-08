package org.github.autojs.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import org.autojs.autojs.ui.log.LogScreen
import org.github.autojs.ui.main.task.TaskManagerScreen
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stardust.autojs.compose.theme.AppTheme
import kotlinx.coroutines.launch
import org.autojs.autojs.R

private data class MainTab(
    val titleRes: Int,
)

private val tabs = listOf(
    MainTab(R.string.text_file),
    MainTab(R.string.text_tutorial),
    MainTab(R.string.text_manage),
    MainTab(R.string.text_log),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContentPlaceholder()
            }
        },
        modifier = modifier,
    ) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(tab.titleRes)) },
                        )
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> ScriptsPlaceholder()
                        1 -> DocsPlaceholder()
                        2 -> TaskManagerScreen()
                        3 -> LogScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: search */ }) {
                Icon(Icons.Rounded.Search, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

@Composable
private fun DrawerContentPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text("Drawer", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun ScriptsPlaceholder(modifier: Modifier = Modifier) {
    PlaceholderBox(label = "Scripts", modifier = modifier)
}

@Composable
private fun DocsPlaceholder(modifier: Modifier = Modifier) {
    PlaceholderBox(label = "Docs", modifier = modifier)
}

@Composable
private fun PlaceholderBox(label: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    AppTheme { MainScreen() }
}
