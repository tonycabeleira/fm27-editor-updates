package com.cabincrew.academy.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cabincrew.academy.ui.categories.CategoriesScreen
import com.cabincrew.academy.ui.components.isMediumLayout
import com.cabincrew.academy.ui.dashboard.DashboardScreen
import com.cabincrew.academy.ui.exam.ExamResultScreen
import com.cabincrew.academy.ui.exam.ExamSessionScreen
import com.cabincrew.academy.ui.exam.ExamSetupScreen
import com.cabincrew.academy.ui.importexport.ImportExportScreen
import com.cabincrew.academy.ui.questions.QuestionBankScreen
import com.cabincrew.academy.ui.questions.QuestionEditorScreen
import com.cabincrew.academy.ui.settings.SettingsScreen
import com.cabincrew.academy.ui.stats.StatisticsScreen
import com.cabincrew.academy.ui.study.StudySessionScreen
import com.cabincrew.academy.ui.study.StudySetupScreen
import com.cabincrew.academy.ui.theme.AppTheme

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    TopDestination(Routes.STUDY_SETUP, "Study", Icons.AutoMirrored.Filled.MenuBook),
    TopDestination(Routes.EXAM_SETUP, "Exam", Icons.Filled.Timer),
    TopDestination(Routes.QUESTIONS, "Questions", Icons.AutoMirrored.Filled.LibraryBooks),
    TopDestination(Routes.STATISTICS, "Statistics", Icons.Filled.Insights),
    TopDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun CabinCrewApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showNav = topDestinations.any { it.route == currentRoute }
    val navigateTop: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isMediumLayout()) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (showNav) {
                AppNavigationRail(currentRoute, navigateTop)
                VerticalDivider(color = AppTheme.extended.cardBorder)
            }
            Box(Modifier.weight(1f)) { AppNavHost(nav, navigateTop) }
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showNav) NavigationBar {
                    topDestinations.forEach { d ->
                        NavigationBarItem(
                            selected = currentRoute == d.route,
                            onClick = { navigateTop(d.route) },
                            icon = { Icon(d.icon, null) },
                            label = { Text(d.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).consumeWindowInsets(padding)) { AppNavHost(nav, navigateTop) }
        }
    }
}

@Composable
private fun AppNavigationRail(currentRoute: String?, onNavigate: (String) -> Unit) {
    val ext = AppTheme.extended
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(ext.heroStart, ext.heroEnd))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Flight, "Cabin Crew Academy", tint = ext.gold) }
                Spacer(Modifier.height(4.dp))
                Text("CCA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        },
    ) {
        topDestinations.forEach { d ->
            NavigationRailItem(
                selected = currentRoute == d.route,
                onClick = { onNavigate(d.route) },
                icon = { Icon(d.icon, null) },
                label = { Text(d.label) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, navigateTop: (String) -> Unit) {
    NavHost(
        navController = nav,
        startDestination = Routes.DASHBOARD,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { nav.navigate(it) },
                onNavigateTop = navigateTop,
            )
        }
        composable(Routes.STUDY_SETUP) {
            StudySetupScreen(onStart = { config -> nav.navigate(Routes.study(config)) }, onImport = { nav.navigate(Routes.IMPORT) })
        }
        composable(
            Routes.STUDY,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "ALL" },
                navArgument("category") { type = NavType.LongType; defaultValue = -1L },
                navArgument("difficulty") { type = NavType.StringType; defaultValue = "-" },
                navArgument("shuffle") { type = NavType.BoolType; defaultValue = false },
                navArgument("seed") { type = NavType.LongType; defaultValue = 0L },
                navArgument("start") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            StudySessionScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.EXAM_SETUP) {
            ExamSetupScreen(
                onStart = { count, minutes, pass, cats -> nav.navigate(Routes.exam(count, minutes, pass, cats)) },
                onOpenResult = { nav.navigate(Routes.examResult(it)) },
                onImport = { nav.navigate(Routes.IMPORT) },
            )
        }
        composable(
            Routes.EXAM,
            arguments = listOf(
                navArgument("count") { type = NavType.IntType; defaultValue = 20 },
                navArgument("minutes") { type = NavType.IntType; defaultValue = 30 },
                navArgument("pass") { type = NavType.IntType; defaultValue = 75 },
                navArgument("categories") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            ExamSessionScreen(
                onExit = { nav.popBackStack() },
                onFinished = { id ->
                    nav.navigate(Routes.examResult(id)) {
                        popUpTo(Routes.EXAM) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.EXAM_RESULT, arguments = listOf(navArgument("examId") { type = NavType.LongType })) {
            ExamResultScreen(
                onBack = { nav.popBackStack() },
                onNewExam = {
                    nav.navigate(Routes.EXAM_SETUP) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.QUESTIONS) {
            QuestionBankScreen(
                onEdit = { nav.navigate(Routes.editQuestion(it)) },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onImport = { nav.navigate(Routes.IMPORT) },
            )
        }
        composable(
            Routes.EDIT_QUESTION,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) {
            QuestionEditorScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                onBack = { nav.popBackStack() },
                onStudyCategory = { cfg -> nav.navigate(Routes.study(cfg)) },
            )
        }
        composable(Routes.IMPORT) {
            ImportExportScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onOpenExam = { nav.navigate(Routes.examResult(it)) },
                onStudy = { cfg -> nav.navigate(Routes.study(cfg)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onImportExport = { nav.navigate(Routes.IMPORT) }, onCategories = { nav.navigate(Routes.CATEGORIES) })
        }
    }
}
