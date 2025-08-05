package com.ukhvat.notes

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Removed Hilt imports for Koin migration
import com.ukhvat.notes.ui.screens.NotesListViewModel
import com.ukhvat.notes.ui.screens.NotesListScreen
import com.ukhvat.notes.ui.screens.NoteEditScreen
import com.ukhvat.notes.ui.screens.VersionHistoryScreen
import com.ukhvat.notes.ui.screens.TrashScreen



import com.ukhvat.notes.data.datasource.SearchResultInfo
import com.ukhvat.notes.ui.theme.ThemedUkhvat
import com.ukhvat.notes.ui.theme.ColorManager
import com.ukhvat.notes.ui.theme.UkhvatTheme
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition



/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Removed @AndroidEntryPoint annotation as Koin uses runtime DI
 * without requiring compile-time annotation processing
 */
class MainActivity : AppCompatActivity() {
    
    fun updateSystemBars(isDarkTheme: Boolean) {
        // System panel color setup
        // Status bar: dark blue (darker than main blue color)
        window.statusBarColor = Color(0xFF1564c0).toArgb()
        
        // Navigation panel: adaptive color based on theme
        window.navigationBarColor = if (!isDarkTheme) {
            Color.White.toArgb()  // Light theme - white navigation panel
        } else {
            Color(0xFF323231).toArgb()  // Dark theme - dark navigation panel
        }
        
        // System panel icon color setup
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false // Status bar always dark background - light icons
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme // Light theme: gray buttons on white background
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            // Get single ViewModel and pass it everywhere (migrated to Koin)
            val viewModel: NotesListViewModel = koinViewModel()
            
            ThemedUkhvat(viewModel = viewModel) { isDarkTheme ->
                // Make system panel updates reactive to theme changes
                LaunchedEffect(isDarkTheme) {
                    updateSystemBars(isDarkTheme)
                }
                
                MainNavigation(viewModel = viewModel)
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle language change smoothly without screen flashing
        // The system will automatically update the UI with new locale
    }
}





@Composable
fun MainNavigation(
    viewModel: NotesListViewModel // Accept ViewModel as parameter
) {
                UkhvatTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            
            NavHost(
                navController = navController,
                startDestination = "notes_list",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                composable("notes_list") {
                    // Get search clearing information from navigation
                    val shouldClearSearch = navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.get<Boolean>("shouldClearSearch") ?: false
                    
                    // Get scroll to top information from navigation
                    val shouldScrollToTop = navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.get<Boolean>("shouldScrollToTop") ?: false
                        
                    NotesListScreen(
                        viewModel = viewModel, // Pass ViewModel
                        onNavigateToNote = { noteId, searchInfo ->
                            if (searchInfo != null) {
                                // Navigation with search information
                                val encodedQuery = Uri.encode(searchInfo.searchQuery)
                                navController.navigate("edit_note/$noteId?searchQuery=$encodedQuery&searchPosition=${searchInfo.foundPosition}")
                            } else {
                                // Regular navigation
                                navController.navigate("edit_note/$noteId")
                            }
                        },
                        onNavigateToNewNote = {
                                    // New note is created via NotesListViewModel.createNewNote()
        // This function should not be called
                        },
                        onNavigateToTrash = {
                            navController.navigate("trash")
                        },
                        shouldClearSearch = shouldClearSearch,
                        onClearSearchHandled = {
                            // Clear flag after processing
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("shouldClearSearch", false)
                        },
                        shouldScrollToTop = shouldScrollToTop,
                        onScrollToTopHandled = {
                            // Clear flag after processing
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("shouldScrollToTop", false)
                        }
                    )
                }
                
                composable(
                    "edit_note/{noteId}?searchQuery={searchQuery}&searchPosition={searchPosition}",
                    arguments = listOf(
                        navArgument("noteId") { 
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                        navArgument("searchQuery") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        },
                        navArgument("searchPosition") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                    val searchQuery = backStackEntry.arguments?.getString("searchQuery")?.let { 
                        Uri.decode(it) 
                    } ?: ""
                    val searchPosition = backStackEntry.arguments?.getInt("searchPosition") ?: -1
                    
                    NoteEditScreen(
                        noteId = noteId,
                        navigateBack = { shouldClearMainSearch, shouldScrollToTop ->
                            // Set search clearing flag if needed
                            if (shouldClearMainSearch) {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("shouldClearSearch", true)
                            }
                            // Set scroll to top flag if needed
                            if (shouldScrollToTop) {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("shouldScrollToTop", true)
                            }
                            navController.popBackStack()
                        },
                        navigateToVersionHistory = { noteId ->
                            navController.navigate("version_history/$noteId")
                        },

                        // Pass search info if available
                        initialSearchQuery = if (searchQuery.isNotEmpty()) searchQuery else null,
                        initialSearchPosition = if (searchPosition != -1) searchPosition else null
                    )
                }
                
                composable(
                    "version_history/{noteId}",
                    arguments = listOf(
                        navArgument("noteId") { 
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                    VersionHistoryScreen(
                        noteId = noteId,
                        onBackClick = { 
                            navController.popBackStack()
                        },
                        onVersionClick = { /* Not used - versions selected via dialogs */ },
                        onNavigateToNewNote = { newNoteId ->
                            // Navigate to new note created from version
                            navController.navigate("edit_note/$newNoteId") {
                                popUpTo("notes_list") { inclusive = false }
                            }
                        }
                    )
                }
                
                composable("trash") {
                    TrashScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
} 