package com.vbrazhnik.obleek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vbrazhnik.obleek.data.ObleekDatabase
import com.vbrazhnik.obleek.data.ObleekRepository
import com.vbrazhnik.obleek.ui.ObleekViewModel
import com.vbrazhnik.obleek.ui.ObleekViewModelFactory
import com.vbrazhnik.obleek.ui.screens.AddAccountDialog
import com.vbrazhnik.obleek.ui.screens.AddDebtDialog
import com.vbrazhnik.obleek.ui.screens.AddRecurringDialog
import com.vbrazhnik.obleek.ui.screens.AddTransactionDialog
import com.vbrazhnik.obleek.ui.screens.AnalyticsScreen
import com.vbrazhnik.obleek.ui.screens.DashboardScreen
import com.vbrazhnik.obleek.ui.screens.DebtsAndRecurringScreen
import com.vbrazhnik.obleek.ui.screens.NotificationsScannerScreen
import com.vbrazhnik.obleek.ui.theme.ObleekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ObleekDatabase.getInstance(applicationContext)
        val repository = ObleekRepository(database.obleekDao())
        val factory = ObleekViewModelFactory(repository)

        setContent {
            val obleekViewModel: ObleekViewModel = viewModel(factory = factory)
            val uiState by obleekViewModel.uiState.collectAsStateWithLifecycle()

            ObleekTheme(darkTheme = uiState.isDarkTheme) {
                ObleekAppRoot(
                    viewModel = obleekViewModel,
                    uiState = uiState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObleekAppRoot(
    viewModel: ObleekViewModel,
    uiState: com.vbrazhnik.obleek.ui.ObleekUiState
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var showAddTransactionType by remember { mutableStateOf<String?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    val navItems = listOf(
        Triple(0, stringResource(R.string.nav_dashboard), Icons.Default.Dashboard),
        Triple(1, stringResource(R.string.nav_scanner), Icons.Default.NotificationsActive),
        Triple(2, stringResource(R.string.nav_debts), Icons.Default.People),
        Triple(3, stringResource(R.string.nav_analytics), Icons.Default.Analytics)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "O/",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Multi-Currency • Smart ML",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleDarkTheme() },
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Przełącz motyw",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddTransactionType = "EXPENSE" },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_transaction_fab)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    )
                }
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        navItems.forEach { (index, label, icon) ->
                            NavigationBarItem(
                                selected = currentTab == index,
                                onClick = { currentTab = index },
                                icon = {
                                    if (index == 1 && uiState.pendingNotifications.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(uiState.pendingNotifications.size.toString())
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_$index")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { (index, label, icon) ->
                            NavigationRailItem(
                                selected = currentTab == index,
                                onClick = { currentTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_$index")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            uiState = uiState,
                            onSelectMainCurrency = viewModel::setMainCurrency,
                            onOpenAddTransaction = { type -> showAddTransactionType = type },
                            onOpenAddAccount = { showAddAccountDialog = true },
                            onNavigateToScanner = { currentTab = 1 },
                            onConfirmNotification = { notif -> viewModel.confirmNotification(notif) },
                            onDismissNotification = viewModel::dismissNotification,
                            onSelectCategoryFilter = viewModel::setCategoryFilter,
                            onDeleteTransaction = viewModel::deleteTransaction
                        )

                        1 -> NotificationsScannerScreen(
                            uiState = uiState,
                            onSimulateOrParseNotification = viewModel::simulateOrParseNotification,
                            onConfirmNotification = { notif -> viewModel.confirmNotification(notif) },
                            onDismissNotification = viewModel::dismissNotification
                        )

                        2 -> DebtsAndRecurringScreen(
                            uiState = uiState,
                            onOpenAddDebt = { showAddDebtDialog = true },
                            onSettleDebt = { debt ->
                                val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)
                                viewModel.settleDebt(
                                    debt = debt,
                                    amount = remaining,
                                    accountId = uiState.accounts.firstOrNull()?.id
                                )
                            },
                            onDeleteDebt = viewModel::deleteDebt,
                            onOpenAddRecurring = { showAddRecurringDialog = true },
                            onPayRecurringNow = viewModel::payRecurringNow,
                            onDeleteRecurring = viewModel::deleteRecurringPayment
                        )

                        3 -> AnalyticsScreen(uiState = uiState)
                    }
                }
            }
        }
    }

    // Dialogs
    showAddTransactionType?.let { type ->
        AddTransactionDialog(
            initialType = type,
            accounts = uiState.accounts,
            onDismiss = { showAddTransactionType = null },
            onConfirm = { title, merchant, amount, currency, txType, category, accountId, targetAccountId ->
                viewModel.addTransaction(
                    title = title,
                    merchant = merchant,
                    amount = amount,
                    currency = currency,
                    type = txType,
                    category = category,
                    accountId = accountId,
                    targetAccountId = targetAccountId
                )
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, accountType, currency, balance, cardLastFour ->
                viewModel.addAccount(
                    name = name,
                    accountType = accountType,
                    currency = currency,
                    balance = balance,
                    cardLastFour = cardLastFour,
                    accentHex = "#38BDF8"
                )
            }
        )
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { personName, totalAmount, currency, isLentByMe, note, dueDays ->
                viewModel.addDebt(
                    personName = personName,
                    totalAmount = totalAmount,
                    currency = currency,
                    isLentByMe = isLentByMe,
                    note = note,
                    dueDays = dueDays
                )
            }
        )
    }

    if (showAddRecurringDialog) {
        AddRecurringDialog(
            accounts = uiState.accounts,
            onDismiss = { showAddRecurringDialog = false },
            onConfirm = { title, amount, currency, category, cycle, isIncome, nextDueInDays, accountId ->
                viewModel.addRecurringPayment(
                    title = title,
                    amount = amount,
                    currency = currency,
                    category = category,
                    cycle = cycle,
                    isIncome = isIncome,
                    nextDueInDays = nextDueInDays,
                    accountId = accountId
                )
            }
        )
    }
}
