package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class IssuedOnLoanActivity : ComponentActivity() {
    private val issuedOnLoanViewModel: IssuedOnLoanViewModel by viewModels { IssuedOnLoanViewModelFactory(application) }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("currency", "UAH") ?: "UAH"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedLanguage = Locale.getDefault().language
        updateLocale(this, selectedLanguage)

        // Використовуйте новий API для керування вікном
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showOverdueMessage by remember { mutableStateOf(false) }
                val selectedCurrency = getSelectedCurrency(LocalContext.current)

                LaunchedEffect(Unit) {
                    issuedOnLoanViewModel.loadTransactions(application)
                    if (issuedOnLoanViewModel.hasOverdueTransactions()) {
                        delay(500)
                        showOverdueMessage = true
                        delay(3000)
                        showOverdueMessage = false
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@IssuedOnLoanActivity, MainActivity::class.java).apply {
                                    putExtra("SHOW_SPLASH_SCREEN", false)
                                }
                                startActivity(intent)
                            },
                            onNavigateToIncomes = { navigateToActivity(IncomeActivity::class.java) },
                            onNavigateToExpenses = { navigateToActivity(ExpenseActivity::class.java) },
                            onNavigateToIssuedOnLoan = { navigateToActivity(IssuedOnLoanActivity::class.java) },
                            onNavigateToBorrowed = { navigateToActivity(BorrowedActivity::class.java) },
                            onNavigateToAllTransactionIncome = { navigateToActivity(AllTransactionIncomeActivity::class.java) },
                            onNavigateToAllTransactionExpense = { navigateToActivity(AllTransactionExpenseActivity::class.java) },
                            onNavigateToBudgetPlanning = { navigateToActivity(BudgetPlanningActivity::class.java) },
                            onNavigateToTaskActivity = { navigateToActivity(TaskActivity::class.java) }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(id = R.string.issued_on_loan), color = Color.White) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu), tint = Color.White)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent) // Змінено на прозорий колір
                            )
                        },
                        content = { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .paint(
                                        painter = painterResource(id = R.drawable.background_app),
                                        contentScale = ContentScale.Crop
                                    )
                                    .padding(innerPadding)
                            ) {
                                IssuedOnLoanScreen(viewModel = issuedOnLoanViewModel, selectedCurrency = selectedCurrency)

                                AnimatedVisibility(
                                    visible = showOverdueMessage,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 100.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.Red.copy(alpha = 0.8f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(stringResource(id = R.string.overdue_loan_message), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun IssuedOnLoanScreen(
    viewModel: IssuedOnLoanViewModel,
    modifier: Modifier = Modifier,
    selectedCurrency: String
) {
    var showAddIssuedOnLoanDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<LoanTransaction?>(null) }
    var transactionToRepayOrAdd by remember { mutableStateOf<LoanTransaction?>(null) }
    val transactions by viewModel.transactions.collectAsState()
    val totalIssuedOnLoan by remember { derivedStateOf { transactions.sumOf { it.amount } } } // Ensure the total updates when transactions change

    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 8.dp else 16.dp

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(transactions) { issuedOnLoanTransaction ->
                        IssuedOnLoanTransactionRow(
                            issuedOnLoanTransaction,
                            viewModel,
                            onEdit = { transactionToEdit = it },
                            onRepayOrAdd = { transactionToRepayOrAdd = it },
                            selectedCurrency = selectedCurrency
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding)
                ) {
                    Text(
                        text = stringResource(id = R.string.total_issued_on_loan),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = padding)
                    )
                    Text(
                        text = "${totalIssuedOnLoan.formatIssuedOnLoanAmount(2)} $selectedCurrency",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            FloatingActionButton(
                onClick = { showAddIssuedOnLoanDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(padding),
                containerColor = Color(0xFFFFA500)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_transaction), tint = Color.White)
            }

            if (showAddIssuedOnLoanDialog || transactionToEdit != null) {
                AddOrEditIssuedOnLoanTransactionDialog(
                    onDismiss = {
                        showAddIssuedOnLoanDialog = false
                        transactionToEdit = null
                    },
                    onSave = { newTransaction ->
                        if (transactionToEdit != null) {
                            viewModel.updateLoanTransaction(newTransaction)
                        } else {
                            viewModel.addLoanTransaction(newTransaction)
                        }
                        transactionToEdit = null
                        showAddIssuedOnLoanDialog = false
                    },
                    transactionToEdit = transactionToEdit
                )
            }

            transactionToRepayOrAdd?.let { transaction ->
                RepayOrAddIssuedOnLoanTransactionDialog(
                    onDismiss = { transactionToRepayOrAdd = null },
                    onRepayOrAdd = { transactionAmount: Double, transactionDate: String, isRepay: Boolean ->
                        if (isRepay) {
                            viewModel.repayLoanTransaction(transaction, transactionAmount, transactionDate)
                        } else {
                            viewModel.addToLoanTransaction(transaction, transactionAmount, transactionDate)
                        }
                        transactionToRepayOrAdd = null
                    },
                    transactionToRepayOrAdd = transaction,
                    onDeleteSubTransaction = { subTransaction: IssuedSubTransaction ->
                        viewModel.deleteIssuedSubTransaction(transaction, subTransaction)
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepayOrAddIssuedOnLoanTransactionDialog(
    onDismiss: () -> Unit,
    onRepayOrAdd: (Double, String, Boolean) -> Unit,
    transactionToRepayOrAdd: LoanTransaction,
    onDeleteSubTransaction: (IssuedSubTransaction) -> Unit
) {
    var transactionAmount by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(getCurrentDateForIssuedOnLoan()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isRepay by remember { mutableStateOf(true) }
    var showAllTransactions by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf(transactionToRepayOrAdd.transactions.toList()) }
    var currentAmount by remember { mutableStateOf(transactionToRepayOrAdd.amount) }

    if (showDatePicker) {
        IssuedOnLoanDatePickerDialog(
            onDateSelected = { selectedDate ->
                transactionDate = selectedDate
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRepay) stringResource(id = R.string.repay_transaction) else stringResource(id = R.string.add_debt),
                style = TextStyle(color = Color.White)
            )
        },
        text = {
            Column {
                Text(
                    text = "${stringResource(id = R.string.current_amount)}: ${currentAmount.formatIssuedOnLoanAmount(2)}",
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = transactionAmount,
                    onValueChange = { transactionAmount = it },
                    label = { Text(if (isRepay) stringResource(id = R.string.repayment_amount) else stringResource(id = R.string.add_debt_amount), style = TextStyle(color = Color.White)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.transaction_date),
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = transactionDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.repay_transaction),
                        style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { isRepay = true }
                            .background(if (isRepay) Color.Gray else Color.Transparent)
                            .padding(8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.add_debt),
                        style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { isRepay = false }
                            .background(if (!isRepay) Color.Gray else Color.Transparent)
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showAllTransactions = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.view_transactions), color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                if (showAllTransactions) {
                    LazyColumn {
                        items(transactions) { subTransaction ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${subTransaction.date}: ${subTransaction.amount.formatIssuedOnLoanAmount(2)}",
                                    style = TextStyle(
                                        color = if (subTransaction.amount < 0) Color.Red else Color.Green,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                IconButton(onClick = {
                                    onDeleteSubTransaction(subTransaction)
                                    transactions = transactions.filter { it != subTransaction }
                                    currentAmount -= subTransaction.amount
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val transactionAmountValue = transactionAmount.replace(",", "").toDoubleOrNull() // Remove commas before converting
                    if (transactionAmountValue != null) {
                        onRepayOrAdd(transactionAmountValue, transactionDate, isRepay)
                    }
                    onDismiss() // Close the dialog
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)), // Set the button color to yellow
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        },
        containerColor = Color.DarkGray
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun IssuedOnLoanTransactionRow(
    issuedOnLoanTransaction: LoanTransaction,
    viewModel: IssuedOnLoanViewModel,
    onEdit: (LoanTransaction) -> Unit,
    onRepayOrAdd: (LoanTransaction) -> Unit,
    selectedCurrency: String
) {
    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 4.dp else 8.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.DarkGray.copy(alpha = 0.9f), // Темно-сірий зліва
                            Color.DarkGray.copy(alpha = 0.1f)  // Майже прозорий справа
                        )
                    )
                )
                .clickable {
                    onRepayOrAdd(issuedOnLoanTransaction)
                }
                .padding(padding)
        ) {
            Column {
                Text(
                    text = "${stringResource(id = R.string.amount)}: ${issuedOnLoanTransaction.amount.formatIssuedOnLoanAmount(2)} $selectedCurrency",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = Color.Red),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.borrower_name)}: ${issuedOnLoanTransaction.borrowerName}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = Color.Red),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.issue_date)}: ${issuedOnLoanTransaction.issueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.due_date)}: ${issuedOnLoanTransaction.dueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.comment)}: ${issuedOnLoanTransaction.comment}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { onEdit(issuedOnLoanTransaction) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = Color.White)
                }
                IconButton(onClick = { viewModel.removeLoanTransaction(issuedOnLoanTransaction) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.White)
                }
            }
        }
    }
}

fun Double.formatIssuedOnLoanAmount(digits: Int): String {
    val formatter = "%,.${digits}f"
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditIssuedOnLoanTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (LoanTransaction) -> Unit,
    transactionToEdit: LoanTransaction? = null
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var borrowerName by remember { mutableStateOf(transactionToEdit?.borrowerName ?: "") }
    var issueDate by remember { mutableStateOf(transactionToEdit?.issueDate ?: getCurrentDateForIssuedOnLoan()) }
    var dueDate by remember { mutableStateOf(transactionToEdit?.dueDate ?: getCurrentDateForIssuedOnLoan()) }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showIssueDatePicker) {
        IssuedOnLoanDatePickerDialog(
            onDateSelected = { selectedDate ->
                issueDate = selectedDate
                showIssueDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        IssuedOnLoanDatePickerDialog(
            onDateSelected = { selectedDate ->
                dueDate = selectedDate
                showDueDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (transactionToEdit != null) stringResource(id = R.string.edit_transaction) else stringResource(id = R.string.add_new_transaction), style = TextStyle(color = Color.White)) },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(id = R.string.loan_amount), style = TextStyle(color = Color.White)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.loan_date),
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showIssueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = issueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.repayment_date),
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = dueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = borrowerName,
                    onValueChange = { borrowerName = it },
                    label = { Text(stringResource(id = R.string.borrower_name_label), style = TextStyle(color = Color.White)) },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(id = R.string.loan_comment), style = TextStyle(color = Color.White)) },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null) {
                        onSave(
                            LoanTransaction(
                                amount = amountValue,
                                borrowerName = borrowerName,
                                issueDate = issueDate,
                                dueDate = dueDate,
                                comment = comment,
                                id = transactionToEdit?.id ?: UUID.randomUUID()
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.save), style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        },
        containerColor = Color.DarkGray
    )
}

@Composable
fun IssuedOnLoanDatePickerDialog(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        },
        year, month, day
    )
    LaunchedEffect(Unit) {
        datePickerDialog.show()
    }
}

fun getCurrentDateForIssuedOnLoan(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

class IssuedOnLoanViewModel(application: Application) : AndroidViewModel(application) {
    private val _transactions = MutableStateFlow<List<LoanTransaction>>(emptyList())
    val transactions: StateFlow<List<LoanTransaction>> = _transactions

    init {
        loadTransactions(application)
    }

    fun loadTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("LoanPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = sharedPreferences.getString("LoanTransactions", "[]")
        val type = object : TypeToken<List<LoanTransaction>>() {}.type
        val loadedTransactions: List<LoanTransaction> = gson.fromJson(transactionsJson, type)
        _transactions.update { loadedTransactions }
    }

    private fun saveTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("LoanPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = gson.toJson(_transactions.value)
        sharedPreferences.edit().putString("LoanTransactions", transactionsJson).apply()
    }

    fun addLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList + transaction
        }
        saveTransactions(getApplication())
    }

    fun addToLoanTransaction(transaction: LoanTransaction, additionalAmount: Double, transactionDate: String) {
        _transactions.update { currentList ->
            currentList.map { it: LoanTransaction ->
                if (it.id == transaction.id) {
                    val updatedTransactions = it.transactions.toMutableList()
                    updatedTransactions.add(IssuedSubTransaction(additionalAmount, transactionDate))
                    it.copy(amount = it.amount + additionalAmount, transactions = updatedTransactions)
                } else {
                    it
                }
            }
        }
        saveTransactions(getApplication())
    }

    fun updateLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList.map {
                if (it.id == transaction.id) {
                    val updatedComment = if (transaction.comment.isEmpty()) it.comment else transaction.comment
                    transaction.copy(comment = updatedComment)
                } else {
                    it
                }
            }
        }
        saveTransactions(getApplication())
    }

    fun addOrUpdateLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            val updatedList = currentList.filter { it.id != transaction.id }
            updatedList + transaction
        }
        saveTransactions(getApplication())
    }

    fun removeLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList - transaction
        }
        saveTransactions(getApplication())
    }

    fun repayLoanTransaction(transaction: LoanTransaction, repaymentAmount: Double, repaymentDate: String) {
        _transactions.update { currentList ->
            currentList.map { it: LoanTransaction ->
                if (it.id == transaction.id) {
                    val updatedTransactions = it.transactions.toMutableList()
                    updatedTransactions.add(IssuedSubTransaction(-repaymentAmount, repaymentDate))
                    it.copy(amount = (it.amount - repaymentAmount).coerceAtLeast(0.0), transactions = updatedTransactions)
                } else {
                    it
                }
            }
        }
        saveTransactions(getApplication())
    }

    fun deleteIssuedSubTransaction(transaction: LoanTransaction, subTransaction: IssuedSubTransaction) {
        _transactions.update { currentList ->
            currentList.map { it: LoanTransaction ->
                if (it.id == transaction.id) {
                    val updatedTransactions = it.transactions.toMutableList()
                    updatedTransactions.remove(subTransaction)
                    val updatedAmount = it.amount - subTransaction.amount
                    it.copy(amount = updatedAmount, transactions = updatedTransactions)
                } else {
                    it
                }
            }
        }
        saveTransactions(getApplication())
    }

    fun hasOverdueTransactions(): Boolean {
        val currentDate = Date()
        return _transactions.value.any { it.dueDate.toIssuedOnLoanDate().before(currentDate) }
    }
}

fun String.toIssuedOnLoanDate(): Date {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.parse(this) ?: Date()
}

class IssuedOnLoanViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IssuedOnLoanViewModel::class.java)) {
            return IssuedOnLoanViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class LoanTransaction(
    val amount: Double,
    val borrowerName: String,
    val issueDate: String,
    val dueDate: String,
    val comment: String,
    val transactions: MutableList<IssuedSubTransaction> = mutableListOf(),
    val id: UUID = UUID.randomUUID()
)

data class IssuedSubTransaction(
    val amount: Double,
    val date: String
)
