package com.xunji.flashcards.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.*
import androidx.navigation.compose.*
import com.xunji.flashcards.AppContainer
import com.xunji.flashcards.ui.components.*
import com.xunji.flashcards.ui.screens.*
import com.xunji.flashcards.ui.theme.XunjiTheme
import kotlinx.coroutines.*
import java.time.LocalDate

private fun Context.activity():Activity?=when(this) { is Activity->this;is ContextWrapper->baseContext.activity();else->null }
@Composable fun XunjiApp(container:AppContainer) {
 val main:MainViewModel=viewModel(factory=remember { viewModelFactory { initializer { MainViewModel(container.repository) } } })
 val review:ReviewViewModel=viewModel(factory=remember { viewModelFactory { initializer { ReviewViewModel(container.repository,container.review,container.scheduler,createSavedStateHandle()) } } })
 val settings:SettingsViewModel=viewModel(factory=remember { viewModelFactory { initializer { SettingsViewModel(container.repository,container.backup,container.files) { review.finish();main.refresh() } } } })
 val state by main.state.collectAsStateWithLifecycle();val settingsState by settings.state.collectAsStateWithLifecycle()
 val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route ?: "Today"
 val owner=LocalLifecycleOwner.current;val context=LocalContext.current
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event -> when(event) { Lifecycle.Event.ON_RESUME->{main.refresh();review.setForeground(true)};Lifecycle.Event.ON_PAUSE->review.setForeground(false);else->Unit } }
  owner.lifecycle.addObserver(observer)
  onDispose {owner.lifecycle.removeObserver(observer)}
 }
 LaunchedEffect(owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while(isActive) { main.refresh();review.refresh();delay(60_000) } } }
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri->if(uri!=null)settings.export(uri) }
 val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri->if(uri!=null)settings.prepareRestore(uri) }
 val root=route in listOf("Today","Decks","Statistics")
 val focused=route.startsWith("Editor") || route.startsWith("Review")
 val unavailable=state.error!=null || settingsState.initializationError!=null
 fun navigateRoot(destination:String) { nav.navigate(destination) { popUpTo("Today") { saveState=true };launchSingleTop=true;restoreState=true } }
 XunjiTheme(state.settings.themeMode) {
  val dark=MaterialTheme.colorScheme.background.red<.3f
  SideEffect { context.activity()?.window?.let { window->WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark } } }
  Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={ if(!focused && !unavailable)Row(Modifier.fillMaxWidth().statusBarsPadding().padding(10.dp,6.dp),verticalAlignment=Alignment.CenterVertically) {
   if(root) { Spacer(Modifier.width(12.dp));Mark();Spacer(Modifier.width(12.dp));Text("循记",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f)) }
   else { IconAction("back","返回",{nav.popBackStack()},"page-back");Text(if(route=="Settings")"设置" else "牌组",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f)) }
   if(route!="Settings")IconAction("settings","设置",{nav.navigate("Settings")},"open-settings")
  } },bottomBar={ if(root && !unavailable)NavigationBar(containerColor=MaterialTheme.colorScheme.background,tonalElevation=0.dp) {
   listOf(Triple("Today","今日","today"),Triple("Decks","牌组","decks"),Triple("Statistics","记录","stats")).forEach { (destination,label,icon)->NavigationBarItem(selected=route==destination,onClick={navigateRoot(destination)},icon={Glyph(icon,color=if(route==destination)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)},label={Text(label)},modifier=Modifier.testTag("nav-$icon"),colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer)) }
  } }) { padding->
   if(unavailable) {
    Box(Modifier.fillMaxSize().padding(padding).padding(22.dp),contentAlignment=Alignment.Center) {
     EmptyState("暂时无法读取数据",settingsState.initializationError ?: state.error.orEmpty(),"重新读取",{main.retryObservation();settings.retryObservation()})
    }
   } else NavHost(nav,startDestination="Today",modifier=Modifier.padding(padding).then(if(focused)Modifier.statusBarsPadding().navigationBarsPadding() else Modifier),enterTransition={fadeIn(tween(200))},exitTransition={fadeOut(tween(180))}) {
    composable("Today") { TodayScreen(state,{nav.navigate("Review/all")},{navigateRoot("Decks")}) }
    composable("Decks") { DecksScreen(state,main) {nav.navigate("DeckDetail/$it")} }
    composable("Statistics") { StatisticsScreen(state) }
    composable("DeckDetail/{deckId}") { backStack->val id=backStack.arguments!!.getString("deckId")!!;DeckDetailScreen(id,state,main,{card->nav.navigate("Editor/$id"+if(card!=null)"?cardId=$card" else "")},{nav.navigate("Review/$id")}) }
    composable("Editor/{deckId}?cardId={cardId}",arguments=listOf(navArgument("cardId") {type=NavType.StringType;nullable=true;defaultValue=null})) { backStack->
     val deckId=backStack.arguments!!.getString("deckId")!!;val cardId=backStack.arguments!!.getString("cardId")
     val editor:EditorViewModel=viewModel(viewModelStoreOwner=backStack,factory=remember(deckId,cardId) {viewModelFactory {initializer {EditorViewModel(container.repository,deckId,cardId,createSavedStateHandle())}}})
     val editorState by editor.state.collectAsStateWithLifecycle()
     CardEditorScreen(editorState,editor,cardId!=null) {nav.popBackStack()}
    }
    composable("Review/{deckId}") { backStack->
     val id=backStack.arguments!!.getString("deckId")!!.takeUnless {it=="all"}
     LaunchedEffect(id) {review.start(id)}
     DisposableEffect(Unit) {onDispose {if(context.activity()?.isChangingConfigurations!=true)review.finish()}}
     val reviewState by review.state.collectAsStateWithLifecycle();val undoable by review.canUndo.collectAsStateWithLifecycle()
     val exit={review.finish();nav.popBackStack();Unit}
     BackHandler {if(reviewState !is ReviewUiState.Submitting)exit()}
     ReviewScreen(reviewState,undoable,review::reveal,review::grade,review::undo,review::refresh,exit)
    }
    composable("Settings") {SettingsScreen(settingsState,settings,{export.launch("循记-${LocalDate.now()}.xunji.json")},{restore.launch(arrayOf("application/json","text/*","application/octet-stream"))})}
   }
  }
 }
}
