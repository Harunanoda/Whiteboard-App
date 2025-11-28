package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme

// 線のデータ定義
data class Line(
    val path: Path,
    val strokeWidth: Float = 10f
)

// 【修正点1】データを保持する「倉庫」を作ります
// ここ（object）に置いておけば、ダークモード切り替えで画面がリセットされてもデータは消えません。
object DrawingData {
    val lines = mutableStateListOf<Line>()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WhiteboardApp()
            }
        }
    }
}

// 画面の定義
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Draw : Screen("draw", "Draw", Icons.Default.Edit)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Draw,
    Screen.Settings
)

@Composable
fun WhiteboardApp() {
    val navController = rememberNavController()

    // 【修正点2】データ倉庫からリストを参照します
    // これで画面が作り直されても、倉庫にあるデータはそのまま残ります
    val lines = DrawingData.lines

    val isSystemInDarkTheme = isSystemInDarkTheme()
    var isWhiteBackgroundPref by remember { mutableStateOf(false) }

    val isBlackboardEffective = isSystemInDarkTheme && !isWhiteBackgroundPref

    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Draw.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Draw.route) {
                DrawingScreen(
                    lines = lines,
                    isBlackboard = isBlackboardEffective
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isSystemInDarkTheme = isSystemInDarkTheme,
                    isWhiteBackgroundPref = isWhiteBackgroundPref,
                    onToggleWhiteBackground = { isWhiteBackgroundPref = it }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DrawingScreen(
    lines: MutableList<Line>,
    isBlackboard: Boolean
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var redrawTrigger by remember { mutableIntStateOf(0) }

    val backgroundColor = if (isBlackboard) Color.Black else Color.White
    val drawColor = if (isBlackboard) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val path = Path().apply { moveTo(offset.x, offset.y) }
                            currentPath = path
                        },
                        onDrag = { change, _ ->
                            val path = currentPath
                            if (path != null) {
                                path.lineTo(change.position.x, change.position.y)
                                redrawTrigger++
                            }
                        },
                        onDragEnd = {
                            currentPath?.let {
                                lines.add(Line(path = it))
                            }
                            currentPath = null
                            redrawTrigger = 0
                        }
                    )
                }
        ) {
            val trigger = redrawTrigger

            lines.forEach { line ->
                drawPath(
                    path = line.path,
                    color = drawColor,
                    style = Stroke(
                        width = line.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            currentPath?.let { path ->
                drawPath(
                    path = path,
                    color = drawColor,
                    style = Stroke(
                        width = 10f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isSystemInDarkTheme: Boolean,
    isWhiteBackgroundPref: Boolean,
    onToggleWhiteBackground: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "白背景モード",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isSystemInDarkTheme) {
                    Text(
                        text = "端末がライトモードのため、常に白背景です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "ダークモード時でも背景を白にします。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = if (!isSystemInDarkTheme) true else isWhiteBackgroundPref,
                onCheckedChange = onToggleWhiteBackground,
                enabled = isSystemInDarkTheme
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WhiteboardPreview() {
    MyApplicationTheme {
        WhiteboardApp()
    }
}