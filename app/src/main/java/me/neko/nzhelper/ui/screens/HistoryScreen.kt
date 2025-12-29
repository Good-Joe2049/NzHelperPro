package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.JsonParser.parseString
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.DetailsDialog
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format("%02d:", hours))
        append(String.format("%02d:%02d", minutes, seconds))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessions = remember { mutableStateListOf<Session>() }
    val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    var editSession by remember { mutableStateOf<Session?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var remarkInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var watchedMovie by remember { mutableStateOf(false) }
    var climax by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(3f) }
    var mood by remember { mutableStateOf("平静") }
    var props by remember { mutableStateOf("手") }

    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var sessionToView by remember { mutableStateOf<Session?>(null) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(importUri)?.use { inputStream ->
                        val jsonStr = inputStream.bufferedReader().readText()
                        val importedSessions = mutableListOf<Session>()

                        var success = false
                        try {
                            val newList: List<Session> =
                                NzApplication.gson.fromJson(jsonStr, sessionsTypeToken)
                            importedSessions.addAll(newList)
                            success = true
                        } catch (_: Exception) {
                            // 新格式失败，继续尝试旧格式
                        }

                        // 如果新格式失败，尝试旧数组格式
                        if (!success) {
                            try {
                                val root = parseString(jsonStr).asJsonArray
                                for (elem in root) {
                                    if (elem.isJsonArray) {
                                        val arr = elem.asJsonArray
                                        val timeStr = arr[0].asString
                                        val timestamp = LocalDateTime.parse(
                                            timeStr,
                                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        )

                                        val duration = if (arr.size() > 1) arr[1].asInt else 0
                                        val remark =
                                            if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                                        val location =
                                            if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else ""
                                        val watchedMovie =
                                            if (arr.size() > 4) arr[4].asBoolean else false
                                        val climax = if (arr.size() > 5) arr[5].asBoolean else false
                                        val rating = if (arr.size() > 6 && !arr[6].isJsonNull)
                                            arr[6].asFloat.coerceIn(0f, 5f) else 3f
                                        val mood =
                                            if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静"
                                        val props =
                                            if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"

                                        importedSessions.add(
                                            Session(
                                                timestamp = timestamp,
                                                duration = duration,
                                                remark = remark,
                                                location = location,
                                                watchedMovie = watchedMovie,
                                                climax = climax,
                                                rating = rating,
                                                mood = mood,
                                                props = props
                                            )
                                        )
                                    }
                                }
                            } catch (parseException: Exception) {
                                parseException.printStackTrace()
                            }
                        }

                        // 统一处理导入结果
                        if (importedSessions.isNotEmpty()) {
                            sessions.clear()
                            sessions.addAll(importedSessions)
                            SessionRepository.saveSessions(context, sessions)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "成功导入 ${importedSessions.size} 条记录",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "导入失败：文件格式不正确", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(exportUri)?.use { os ->
                        OutputStreamWriter(os).use { writer ->
                            writer.write(NzApplication.gson.toJson(sessions))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 加载历史记录
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("历史记录") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出数据") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("NzHelper_export_${System.currentTimeMillis()}.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入数据（将覆盖当前）") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清除全部记录") },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("(。・ω・。)", style = MaterialTheme.typography.titleLarge)
                        Text("暂无历史记录哦！", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(sessions, key = { it.timestamp }) { session ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            onClick = { sessionToView = session }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            session.timestamp.format(
                                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                            )
                                        )
                                        Text("持续: ${formatTime(session.duration)}")
                                        if (session.remark.isNotBlank()) {
                                            Text(
                                                "备注: ${session.remark}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { sessionToDelete = session }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 删除确认
            sessionToDelete?.let { session ->
                AlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    title = { Text("删除记录") },
                    text = { Text("确认删除此记录吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.remove(session)
                            scope.launch { SessionRepository.saveSessions(context, sessions) }
                            sessionToDelete = null
                        }) { Text("删除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { sessionToDelete = null }) { Text("取消") }
                    }
                )
            }

            // 清除全部确认
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("清除全部记录") },
                    text = { Text("此操作不可撤销，确定要删除所有记录吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.clear()
                            scope.launch { SessionRepository.saveSessions(context, sessions) }
                            showClearDialog = false
                        }) { Text("删除全部") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) { Text("取消") }
                    }
                )
            }

            // 查看详情
            sessionToView?.let { session ->
                Dialog(onDismissRequest = { sessionToView = null }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "会话详情",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            HorizontalDivider()

                            DetailRow(
                                "开始时间",
                                session.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            )
                            DetailRow("持续时长", formatTime(session.duration))
                            DetailRow("地点", session.location.ifEmpty { "无" })
                            DetailRow("备注", session.remark.ifEmpty { "无" })
                            DetailRow("观看小电影", if (session.watchedMovie) "是" else "否")
                            DetailRow("发射", if (session.climax) "是" else "否")
                            DetailRow("道具", session.props)
                            DetailRow("评分", "%.1f / 5.0".format(session.rating))
                            DetailRow("心情", session.mood)

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                TextButton(onClick = { sessionToView = null }) {
                                    Text("关闭")
                                }
                                Button(onClick = {
                                    // 进入编辑模式
                                    editSession = session
                                    isEditing = true
                                    remarkInput = session.remark
                                    locationInput = session.location
                                    watchedMovie = session.watchedMovie
                                    climax = session.climax
                                    rating = session.rating
                                    mood = session.mood
                                    props = session.props
                                    showDetailsDialog = true
                                    sessionToView = null
                                }) {
                                    Text("编辑")
                                }
                            }
                        }
                    }
                }
            }

            // 编辑 / 新增 复用新版 DetailsDialog
            DetailsDialog(
                show = showDetailsDialog,
                remark = remarkInput,
                onRemarkChange = { remarkInput = it },
                location = locationInput,
                onLocationChange = { locationInput = it },
                watchedMovie = watchedMovie,
                onWatchedMovieChange = { watchedMovie = it },
                climax = climax,
                onClimaxChange = { climax = it },
                props = props,
                onPropsChange = { props = it },
                rating = rating,
                onRatingChange = { rating = it },
                mood = mood,
                onMoodChange = { mood = it },
                onConfirm = {
                    if (isEditing && editSession != null) {
                        val index = sessions.indexOf(editSession)
                        if (index != -1) {
                            sessions[index] = editSession!!.copy(
                                remark = remarkInput,
                                location = locationInput,
                                watchedMovie = watchedMovie,
                                climax = climax,
                                rating = rating,
                                mood = mood,
                                props = props
                            )
                        }
                    }

                    scope.launch {
                        SessionRepository.saveSessions(context, sessions)
                    }

                    // 重置状态
                    showDetailsDialog = false
                    isEditing = false
                    editSession = null
                    // 可以不重置输入框，因为下次编辑会覆盖
                },
                onDismiss = {
                    showDetailsDialog = false
                    isEditing = false
                    editSession = null
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
