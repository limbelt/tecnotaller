package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.WorkshopRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderFormScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.TecnoTallerTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WorkshopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TecnoTallerTheme {
                AppNavigation()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private object Routes {
    const val ORDERS = "orders"
    const val INVENTORY = "inventory"
    const val CLIENTS = "clients"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val ORDER_FORM = "order_form?orderId={orderId}&clientId={clientId}"

    fun orderDetail(orderId: String) = "order_detail/$orderId"
    fun orderForm(orderId: String? = null, clientId: String? = null): String {
        val params = buildList {
            if (!orderId.isNullOrBlank()) add("orderId=$orderId")
            if (!clientId.isNullOrBlank()) add("clientId=$clientId")
        }.joinToString("&")
        return if (params.isEmpty()) "order_form" else "order_form?$params"
    }
}

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ORDERS(Routes.ORDERS, R.string.nav_orders, Icons.Filled.Build, Icons.Outlined.Build),
    INVENTORY(Routes.INVENTORY, R.string.nav_inventory, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    CLIENTS(Routes.CLIENTS, R.string.nav_clients, Icons.Filled.People, Icons.Outlined.People)
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            WorkshopApp(currentUser = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopApp(currentUser: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: WorkshopViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WorkshopViewModel(WorkshopRepository(db), currentUser.uid)
            }
        }
    )

    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val partsState by viewModel.partsState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopTab = currentRoute in TopTab.entries.map { it.route }

    var showPartCreateSheet by remember { mutableStateOf(false) }
    var showClientCreateSheet by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    val allOrders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val allParts = (partsState as? UiState.Success)?.data ?: emptyList()
    val allClients = (clientsState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isTopTab) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    Routes.INVENTORY -> "Inventario de repuestos"
                                    Routes.CLIENTS -> "Directorio de clientes"
                                    else -> stringResource(R.string.app_name)
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showProfileMenu = true },
                                modifier = Modifier.testTag("profile_menu_button")
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initial = (currentUser.displayName ?: currentUser.email ?: "T")
                                            .firstOrNull()?.uppercase() ?: "T"
                                        Text(
                                            text = initial,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false }
                            ) {
                                if (!currentUser.email.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = { Text(currentUser.email!!) },
                                        onClick = { showProfileMenu = false },
                                        enabled = false
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_sign_out)) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    },
                                    onClick = {
                                        showProfileMenu = false
                                        signOut(credentialManager = credentialManager, scope = scope)
                                    }
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopTab && !useRail) {
                    NavigationBar {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isTopTab) {
                    val fabLabel = when (currentRoute) {
                        Routes.INVENTORY -> stringResource(R.string.action_new_part)
                        Routes.CLIENTS -> stringResource(R.string.action_new_client)
                        else -> stringResource(R.string.action_new_order)
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (currentRoute) {
                                Routes.INVENTORY -> showPartCreateSheet = true
                                Routes.CLIENTS -> showClientCreateSheet = true
                                else -> navController.navigate(Routes.orderForm())
                            }
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(fabLabel) },
                        modifier = Modifier.testTag("main_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopTab && useRail) {
                    NavigationRail {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.ORDERS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.ORDERS) {
                        OrdersScreen(
                            ordersState = ordersState,
                            onOrderClick = { orderId ->
                                navController.navigate(Routes.orderDetail(orderId))
                            },
                            onAdvanceStatus = { order, nextStatus ->
                                viewModel.updateOrderStatus(order.id, nextStatus)
                            },
                            onCreateOrderClick = {
                                navController.navigate(Routes.orderForm())
                            }
                        )
                    }

                    composable(Routes.INVENTORY) {
                        InventoryScreen(
                            partsState = partsState,
                            onAdjustStock = { part, delta ->
                                viewModel.adjustPartStock(part, delta)
                            },
                            onSavePart = { part, onComplete ->
                                viewModel.savePart(part, onComplete)
                            },
                            onDeletePart = { partId ->
                                viewModel.deletePart(partId)
                            },
                            showCreateSheet = showPartCreateSheet,
                            onDismissCreateSheet = { showPartCreateSheet = false },
                            onOpenCreateSheet = { showPartCreateSheet = true }
                        )
                    }

                    composable(Routes.CLIENTS) {
                        ClientsScreen(
                            clientsState = clientsState,
                            orders = allOrders,
                            onSaveClient = { client, onComplete ->
                                viewModel.saveClient(client, onComplete)
                            },
                            onDeleteClient = { clientId ->
                                viewModel.deleteClient(clientId)
                            },
                            onCreateOrderForClient = { client ->
                                navController.navigate(Routes.orderForm(clientId = client.id))
                            },
                            showCreateSheet = showClientCreateSheet,
                            onDismissCreateSheet = { showClientCreateSheet = false },
                            onOpenCreateSheet = { showClientCreateSheet = true }
                        )
                    }

                    composable(
                        route = Routes.ORDER_DETAIL,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { entry ->
                        val orderId = entry.arguments?.getString("orderId").orEmpty()
                        val order = allOrders.firstOrNull { it.id == orderId }
                        OrderDetailScreen(
                            order = order,
                            availableParts = allParts,
                            onBack = { navController.popBackStack() },
                            onEditOrder = { id ->
                                navController.navigate(Routes.orderForm(orderId = id))
                            },
                            onUpdateStatus = { id, status ->
                                viewModel.updateOrderStatus(id, status)
                            },
                            onAttachPart = { id, partId, qty ->
                                viewModel.attachPartToOrder(id, partId, qty)
                            },
                            onRemovePart = { id, partId ->
                                viewModel.removePartFromOrder(id, partId)
                            },
                            onDeleteOrder = { id ->
                                viewModel.deleteOrder(id) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(
                        route = Routes.ORDER_FORM,
                        arguments = listOf(
                            navArgument("orderId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("clientId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val orderId = entry.arguments?.getString("orderId")
                        val clientId = entry.arguments?.getString("clientId")
                        val existingOrder = orderId?.let { id -> allOrders.firstOrNull { it.id == id } }
                        val preselectedClient = clientId?.let { id -> allClients.firstOrNull { it.id == id } }
                        OrderFormScreen(
                            existingOrder = existingOrder,
                            preselectedClient = preselectedClient,
                            clients = allClients,
                            isMutating = isMutating,
                            onBack = { navController.popBackStack() },
                            onSave = { order, saveClientToDir ->
                                viewModel.saveOrder(order, saveClientToDir) { success, _ ->
                                    if (success) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
