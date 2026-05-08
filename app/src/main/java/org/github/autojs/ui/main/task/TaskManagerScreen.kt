package org.github.autojs.ui.main.task

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stardust.autojs.script.AutoFileSource
import org.autojs.autojs.R
import org.autojs.autojs.ui.main.task.Task
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity

@Composable
fun TaskManagerScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskManagerViewModel = viewModel(),
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.startObserving()
        onDispose { viewModel.stopObserving() }
    }

    val hasRunningTasks = viewModel.runningTasks.isNotEmpty()
    val hasPendingTasks = viewModel.pendingTasks.isNotEmpty()
    val showEmptyState = !hasRunningTasks && !hasPendingTasks

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        if (showEmptyState) {
            EmptyStateView()
        } else {
            TaskList(
                viewModel = viewModel,
                context = context,
                hasRunningTasks = hasRunningTasks,
                hasPendingTasks = hasPendingTasks,
            )
        }
    }
}

@Composable
private fun EmptyStateView(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.notice_no_running_script),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TaskList(
    viewModel: TaskManagerViewModel,
    context: Context,
    hasRunningTasks: Boolean,
    hasPendingTasks: Boolean,
    modifier: Modifier = Modifier,
) {
    val runningTaskTitle = stringResource(R.string.text_running_task)
    val timedTaskTitle = stringResource(R.string.text_timed_task)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (hasRunningTasks) {
            taskGroupSection(
                groupKey = "running_group",
                title = runningTaskTitle,
                expanded = viewModel.runningGroupExpanded,
                onToggle = { viewModel.toggleRunningGroup() },
                onStopClick = { viewModel.stopRunningGroup() },
                showStopButton = viewModel.runningTasks.isNotEmpty(),
                tasks = viewModel.runningTasks,
                onTaskStopClick = { viewModel.stopTask(it) },
                onTaskClick = {},
            )
        }

        if (hasRunningTasks && hasPendingTasks) {
            item(key = "divider_running_pending") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        if (hasPendingTasks) {
            taskGroupSection(
                groupKey = "pending_group",
                title = timedTaskTitle,
                expanded = viewModel.pendingGroupExpanded,
                onToggle = { viewModel.togglePendingGroup() },
                onStopClick = { viewModel.stopPendingGroup() },
                showStopButton = viewModel.pendingTasks.isNotEmpty(),
                tasks = viewModel.pendingTasks,
                onTaskStopClick = { viewModel.stopTask(it) },
                onTaskClick = { task ->
                    navigateToTaskSetting(context, task as Task.PendingTask)
                },
            )
        }
    }
}

private fun LazyListScope.taskGroupSection(
    groupKey: String,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStopClick: () -> Unit,
    showStopButton: Boolean,
    tasks: List<Task>,
    onTaskStopClick: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
) {
    item(key = groupKey) {
        TaskGroupItem(
            title = title,
            expanded = expanded,
            onToggle = onToggle,
            onStopClick = onStopClick,
            showStopButton = showStopButton,
        )
    }
    if (expanded) {
        items(
            count = tasks.size,
            key = { index ->
                val task = tasks[index]
                if (task is Task.PendingTask) "pending_${task.id}"
                else task.scriptExecution.hashCode()
            },
        ) { index ->
            val task = tasks[index]
            TaskItem(
                task = task,
                onStopClick = { onTaskStopClick(task) },
                onClick = { onTaskClick(task) },
            )
        }
    }
}

private fun navigateToTaskSetting(context: Context, task: Task.PendingTask) {
    val extra = when {
        task.timedTask != null -> TimedTaskSettingActivity.EXTRA_TASK_ID
        else -> TimedTaskSettingActivity.EXTRA_INTENT_TASK_ID
    }
    val intent = Intent(context, TimedTaskSettingActivity::class.java)
        .putExtra(extra, task.id)
    context.startActivity(intent)
}

@Composable
private fun TaskGroupItem(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStopClick: () -> Unit,
    showStopButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrow_rotation",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .rotate(arrowRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (showStopButton) {
            IconButton(onClick = onStopClick) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onStopClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAutoEngine = task.engineName == AutoFileSource.ENGINE
    val avatarColor = if (isAutoEngine) Color(0xFFFD999A) else Color(0xFF99CC99)
    val avatarText = if (isAutoEngine) "R" else "J"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
        ) {
            Text(
                text = avatarText,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = task.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onStopClick) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val Task.scriptExecution
    get() = (this as Task.RunningTask).scriptExecution
