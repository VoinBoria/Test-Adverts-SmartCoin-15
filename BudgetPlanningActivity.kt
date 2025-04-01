package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import android.app.DatePickerDialog
import androidx.compose.material.icons.filled.CalendarToday
import java.text.SimpleDateFormat

class BudgetPlanningActivity : ComponentActivity() {
    private val viewModel: BudgetPlanningViewModel by viewModels()

    private lateinit var updateReceiver: BroadcastReceiver
    private lateinit var localeReceiver: BroadcastReceiver

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

    private fun getSelectedLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
        return sharedPreferences.getString("language", "UK") ?: "UK"
    }

    private fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
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
                val selectedCurrency = getSelectedCurrency(LocalContext.current)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@BudgetPlanningActivity, MainActivity::class.java).apply {
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
                                title = {
                                    Text(stringResource(id = R.string.budget_planning), color = Color.White)
                                },
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
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Spacer(modifier = Modifier.height(40.dp))
                                    BudgetPlanningScreen(viewModel, selectedCurrency)
                                }
                            }
                        }
                    )
                }
            }
        }

        updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.homeaccountingapp.UPDATE_EXPENSES") {
                    viewModel.loadExpensesFromMainActivity(context)
                }
            }
        }
        val filter = IntentFilter("com.example.homeaccountingapp.UPDATE_EXPENSES")
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)

        localeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.homeaccountingapp.UPDATE_LOCALE") {
                    val language = intent.getStringExtra("language") ?: return
                    updateLocale(context, language)
                    recreate()
                }
            }
        }
        val localeFilter = IntentFilter("com.example.homeaccountingapp.UPDATE_LOCALE")
        LocalBroadcastManager.getInstance(this).registerReceiver(localeReceiver, localeFilter)

        viewModel.loadExpenseCategories(this)
        viewModel.loadMaxExpenses(this)
        viewModel.loadExpensesFromMainActivity(this)
        viewModel.loadIncomesFromMainActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeReceiver)
    }
}
class ThousandSeparatorVisualTransformationWithSpace : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = originalText.reversed().chunked(3).joinToString(" ").reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val spacesBefore = (0 until offset).count { originalText.reversed().chunked(3).joinToString(" ").reversed()[it].isWhitespace() }
                return offset + spacesBefore
            }

            override fun transformedToOriginal(offset: Int): Int {
                val spacesBefore = (0 until offset).count { formattedText[it].isWhitespace() }
                return offset - spacesBefore
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
class BudgetPlanningViewModel(application: Application) : AndroidViewModel(application) {
    val expenseCategories = MutableLiveData<Map<String, Double>>(emptyMap())
    val maxExpenses = MutableLiveData<Map<String, Double>>(emptyMap())
    val expenses = MutableLiveData<Map<String, Double>>(emptyMap())
    val incomes = MutableLiveData<Map<String, Double>>(emptyMap())
    val savedAmountsWithDates = MutableLiveData<List<Pair<Double, String>>>(emptyList())
    val saveMessage = MutableLiveData<String?>(null)
    val isAddingLimit = MutableLiveData<Boolean>(false)
    val isAddingGoal = MutableLiveData<Boolean>(false)
    var currentCategory: String? = null

    var goalAmount by mutableStateOf("")
    var goalPeriod by mutableStateOf("")
    var weeklySaving by mutableStateOf("")
    var monthlySaving by mutableStateOf("")
    var savedAmount by mutableStateOf("")
    var selectedDate by mutableStateOf("")

    init {
        loadSavedAmounts(application.applicationContext)
    }

    fun loadExpenseCategories(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("categories", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val categories: List<String> = gson.fromJson(json, type)

        val categoriesMap = categories.associateWith { 0.0 }
        expenseCategories.value = categoriesMap
    }

    fun loadMaxExpenses(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("max_expenses", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val maxExpensesMap: Map<String, Double> = gson.fromJson(json, type)
        maxExpenses.value = maxExpensesMap
    }

    fun loadExpensesFromMainActivity(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("expenses", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val expensesMap: Map<String, Double> = gson.fromJson(json, type)
        expenses.value = expensesMap
    }

    fun loadIncomesFromMainActivity(context: Context) {
        val sharedPreferences = context.getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("incomes", "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val incomesMap: Map<String, Double> = gson.fromJson(json, type)
        incomes.value = incomesMap
    }

    fun loadSavedAmounts(context: Context) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_amounts_with_dates", "[]")
        val type = object : TypeToken<List<Pair<Double, String>>>() {}.type
        val savedAmountsList: List<Pair<Double, String>> = gson.fromJson(json, type)
        savedAmountsWithDates.value = savedAmountsList
    }

    fun saveSavedAmounts(context: Context, savedAmountsList: List<Pair<Double, String>>) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(savedAmountsList)
        sharedPreferences.edit().putString("saved_amounts_with_dates", json).apply()
    }

    fun updateMaxExpense(context: Context, category: String, maxExpense: Double) {
        val currentMaxExpenses = maxExpenses.value ?: emptyMap()
        val updatedMaxExpenses = currentMaxExpenses.toMutableMap()
        updatedMaxExpenses[category] = maxExpense
        maxExpenses.value = updatedMaxExpenses

        saveMaxExpenses(context, updatedMaxExpenses)
        saveMessage.value = context.getString(R.string.limit_saved) // Ensure this string resource is defined
        isAddingLimit.value = false
    }

    private fun saveMaxExpenses(context: Context, maxExpenses: Map<String, Double>) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(maxExpenses)
        sharedPreferences.edit().putString("max_expenses", json).apply()
        saveMessage.value = context.getString(R.string.limit_saved)
    }

    fun toggleAddingLimit(category: String) {
        currentCategory = category
        isAddingLimit.value = !(isAddingLimit.value ?: false)
    }

    fun toggleAddingGoal() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        if (!isAddingGoal.value!!) {
            goalAmount = sharedPreferences.getString("goal_amount", "") ?: ""
            goalPeriod = sharedPreferences.getString("goal_period", "") ?: ""
            weeklySaving = sharedPreferences.getString("weekly_saving", "") ?: ""
            monthlySaving = sharedPreferences.getString("monthly_saving", "") ?: ""
            savedAmount = "" // Скидання поля вводу збереження на пусте значення
        }
        isAddingGoal.value = !(isAddingGoal.value ?: false)
    }

    fun calculateGoal() {
        val goalAmountValue = goalAmount.toDoubleOrNull() ?: 0.0
        val goalPeriodValue = goalPeriod.toIntOrNull() ?: 0
        weeklySaving = if (goalPeriodValue > 0) (goalAmountValue / (goalPeriodValue * 4)).formatBudgetAmount(2) else "0.0"
        monthlySaving = if (goalPeriodValue > 0) (goalAmountValue / goalPeriodValue).formatBudgetAmount(2) else "0.0"
    }

    fun saveGoal(context: Context) {
        calculateGoal()
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("goal_amount", goalAmount)
            .putString("goal_period", goalPeriod)
            .putString("weekly_saving", weeklySaving)
            .putString("monthly_saving", monthlySaving)
            .apply()

        saveMessage.value = context.getString(R.string.goal_saved)
        isAddingGoal.value = false
    }

    fun addSaving(context: Context) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        val savedAmountsList = savedAmountsWithDates.value.orEmpty().toMutableList()
        val savedAmountValue = savedAmount.toDoubleOrNull() ?: 0.0
        val currentDate = selectedDate.ifEmpty {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }
        savedAmountsList.add(savedAmountValue to currentDate)
        saveSavedAmounts(context, savedAmountsList)

        savedAmountsWithDates.value = savedAmountsList
        savedAmount = "" // Очистити поле вводу після збереження
        selectedDate = "" // Очистити вибрану дату після збереження
    }

    fun getPercentageToGoal(): Double {
        val goalAmountValue = goalAmount.toDoubleOrNull() ?: 0.0
        val totalSaved = savedAmountsWithDates.value?.sumOf { it.first } ?: 0.0
        return if (goalAmountValue > 0) (totalSaved / goalAmountValue) * 100 else 0.0
    }

    fun updateSaving(context: Context, index: Int, newAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedAmountsList = savedAmountsWithDates.value.orEmpty().toMutableList()
            if (index in savedAmountsList.indices) {
                val currentDate = savedAmountsList[index].second
                savedAmountsList[index] = newAmount to currentDate
                withContext(Dispatchers.Main) {
                    savedAmountsWithDates.value = savedAmountsList
                }
                saveSavingsToPrefs(context, savedAmountsList)
            }
        }
    }

    fun deleteSaving(context: Context, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedAmountsList = savedAmountsWithDates.value.orEmpty().toMutableList()
            if (index in savedAmountsList.indices) {
                savedAmountsList.removeAt(index)
                withContext(Dispatchers.Main) {
                    savedAmountsWithDates.value = savedAmountsList
                }
                saveSavingsToPrefs(context, savedAmountsList)
            }
        }
    }

    private fun saveSavingsToPrefs(context: Context, savings: List<Pair<Double, String>>) {
        val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("saved_amounts_with_dates", Gson().toJson(savings))
            .apply()
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BudgetPlanningScreen(viewModel: BudgetPlanningViewModel, selectedCurrency: String) {
    val expenseCategories by viewModel.expenseCategories.observeAsState(emptyMap())
    val maxExpenses by viewModel.maxExpenses.observeAsState(emptyMap())
    val expenses by viewModel.expenses.observeAsState(emptyMap())
    val saveMessage by viewModel.saveMessage.observeAsState(null)
    val isAddingLimit by viewModel.isAddingLimit.observeAsState(false)
    val isAddingGoal by viewModel.isAddingGoal.observeAsState(false)
    val savingsList by viewModel.savedAmountsWithDates.observeAsState(emptyList()) // Змінили на savedAmountsWithDates
    var isViewingSavings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    saveMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.saveMessage.value = null
    }

    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 10.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 5.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                // Сортування категорій за витратами у зростаючому порядку
                items(expenseCategories.toList().sortedBy { expenses[it.first] ?: 0.0 }) { (category, _) ->
                    val expense = expenses[category] ?: 0.0
                    val maxExpense = maxExpenses[category] ?: 0.0
                    BudgetCategoryItemWithRedBackground(
                        category = category,
                        expense = expense,
                        maxExpense = maxExpense,
                        selectedCurrency = selectedCurrency,
                        onToggleAddingLimit = {
                            viewModel.toggleAddingLimit(category)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Button(
                onClick = {
                    viewModel.toggleAddingGoal()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(padding),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400).copy(alpha = 0.8f))
            ) {
                Text(stringResource(id = R.string.my_goal), color = Color.White)
            }
        }

        if (isAddingLimit) {
            AddLimitDialog(
                category = viewModel.currentCategory ?: "",
                onDismissRequest = { viewModel.isAddingLimit.value = false },
                onSaveMaxExpense = { maxExpense ->
                    viewModel.currentCategory?.let { category ->
                        viewModel.updateMaxExpense(context, category, maxExpense)
                    }
                }
            )
        }

        if (isAddingGoal) {
            val percentageToGoal = viewModel.getPercentageToGoal() // Обчислення прогресу до цілі
            AddGoalDialog(
                goalAmount = viewModel.goalAmount,
                goalPeriod = viewModel.goalPeriod.toFloatOrNull() ?: 0f,
                savedAmount = viewModel.savedAmount, // Передача savedAmount
                onGoalAmountChange = { viewModel.goalAmount = it },
                onGoalPeriodChange = { viewModel.goalPeriod = it.toString() },
                onSavedAmountChange = { viewModel.savedAmount = it },
                onDismissRequest = { viewModel.isAddingGoal.value = false },
                onSaveGoal = {
                    viewModel.saveGoal(context)
                    Toast.makeText(context, context.getString(R.string.goal_saved), Toast.LENGTH_SHORT).show() // Показ повідомлення після збереження
                },
                onAddSaving = { viewModel.addSaving(context) },
                onViewSavings = {
                    viewModel.loadSavedAmounts(context)
                    isViewingSavings = true
                },
                percentageToGoal = percentageToGoal, // Передача прогресу до цілі
                savedAmounts = savingsList.map { it.first }, // Передача списку заощаджень
                selectedCurrency = selectedCurrency, // Передача вибраної валюти
                context = context // Передача контексту
            )
        }

        if (isViewingSavings) {
            SavingsListDialog(
                savingsList = savingsList,
                onDismissRequest = { isViewingSavings = false },
                onUpdateSaving = { index, newAmount -> viewModel.updateSaving(context, index, newAmount) },
                onDeleteSaving = { index -> viewModel.deleteSaving(context, index) },
                selectedCurrency = selectedCurrency // Pass the selectedCurrency
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetCategoryItemWithRedBackground(
    category: String,
    expense: Double,
    maxExpense: Double,
    selectedCurrency: String,
    onToggleAddingLimit: () -> Unit
) {
    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 12.sp else 16.sp
        val padding = if (screenWidth < 360.dp) 4.dp else 8.dp

        // Обчислення прогресу та відсотка витрат
        val progress = if (maxExpense > 0) expense / maxExpense else 0.0
        val percentage = (progress * 100).toInt()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.DarkGray.copy(alpha = 0.9f), // Темно-сірий зліва
                            Color.DarkGray.copy(alpha = 0.1f)  // Майже прозорий справа
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = fontSize),
                    color = Color.White,
                    modifier = Modifier.padding(end = padding)
                )
                TextButton(
                    onClick = onToggleAddingLimit,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    val buttonText = if (maxExpense > 0) stringResource(id = R.string.edit_limit) else stringResource(id = R.string.add_limit)
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(id = R.string.limit)}: ${if (maxExpense > 0) maxExpense.formatBudgetAmount(2) + " " + selectedCurrency else stringResource(id = R.string.limit_not_set)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize),
                    color = Color.Gray
                )
                Text(
                    text = "${stringResource(id = R.string.spent)} $percentage%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Допоміжна функція для форматування чисел
fun Double.formatBudgetAmount(digits: Int): String {
    return "%.${digits}f".format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLimitDialog(
    category: String,
    onDismissRequest: () -> Unit,
    onSaveMaxExpense: (Double) -> Unit,
    textColor: Color = Color.White // Параметр для кольору тексту
) {
    var limitValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black.copy(alpha = 0.8f), // Темний прозорий фон
        title = {
            Text(text = "${stringResource(id = R.string.add_limit)} $category", color = textColor)
        },
        text = {
            OutlinedTextField(
                value = limitValue,
                onValueChange = { value -> limitValue = value },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = textColor // Колір курсора
                ),
                textStyle = LocalTextStyle.current.copy(color = textColor, fontWeight = FontWeight.Bold), // Жирний шрифт
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f)) // Темний прозорий фон для поля вводу
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End // Розташування кнопок праворуч
            ) {
                TextButton(
                    onClick = {
                        val maxExpenseValue = limitValue.toDoubleOrNull() ?: 0.0
                        onSaveMaxExpense(maxExpenseValue)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(id = R.string.save), color = Color.Green, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(id = R.string.cancel), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    goalAmount: String,
    goalPeriod: Float,
    savedAmount: String,
    onGoalAmountChange: (String) -> Unit,
    onGoalPeriodChange: (Float) -> Unit,
    onSavedAmountChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveGoal: () -> Unit,
    onAddSaving: () -> Unit,
    onViewSavings: () -> Unit,
    percentageToGoal: Double,
    savedAmounts: List<Double>,
    selectedCurrency: String,
    context: Context
) {
    val localContext = LocalContext.current
    val scrollState = rememberScrollState()
    var showGoalAmountInput by remember { mutableStateOf(false) }
    val perDay = stringResource(id = R.string.per_day)
    val perWeek = stringResource(id = R.string.per_week)
    val perMonth = stringResource(id = R.string.per_month)
    val perYear = stringResource(id = R.string.per_year)
    val frequencies = listOf(perDay, perWeek, perMonth, perYear)
    var savingFrequency by remember { mutableStateOf(perDay) }
    var calculatedSaving by remember { mutableStateOf(0.0) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") } // Додано для вибору дати

    // Load saved values from SharedPreferences
    val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
    val savedFrequency = sharedPreferences.getString("saving_frequency", perDay) ?: perDay
    val savedCalculatedSaving = sharedPreferences.getFloat("calculated_saving", 0.0f).toDouble()

    // Update state with saved values
    savingFrequency = savedFrequency
    calculatedSaving = savedCalculatedSaving

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(350.dp), // Зробити меню трохи ширшим
        title = {
            Text(stringResource(id = R.string.my_goal), style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier
                .verticalScroll(scrollState)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.DarkGray.copy(alpha = 0.8f), // Темний зліва
                            Color.Blue.copy(alpha = 0.8f) // Синій справа
                        )
                    )
                )
                .padding(horizontal = 16.dp) // Зсунути текст від краю
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    ProgressChart(
                        percentageToGoal = percentageToGoal,
                        modifier = Modifier.size(150.dp) // Встановити розмір діаграми
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = showGoalAmountInput,
                    enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    OutlinedTextField(
                        value = goalAmount,
                        onValueChange = onGoalAmountChange,
                        label = { Text("${stringResource(id = R.string.goal_amount)} ($selectedCurrency)", color = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .align(Alignment.CenterHorizontally),
                        visualTransformation = ThousandSeparatorVisualTransformationWithSpace()
                    )
                }
                if (!showGoalAmountInput) {
                    Button(
                        onClick = { showGoalAmountInput = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.enter_goal_amount), color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(id = R.string.goal_period)}: ${goalPeriod.toInt()} ${stringResource(id = R.string.months)}",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = goalPeriod,
                    onValueChange = onGoalPeriodChange,
                    valueRange = 1f..120f, // Від 1 місяця до 120 місяців
                    steps = 119,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.savings_needed),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    ) {
                        TextButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = savingFrequency,
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        Text(
                            text = " ${calculatedSaving.format(2)} $selectedCurrency",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    savingFrequency = selectionOption
                                    expanded = false
                                    calculatedSaving = when (selectionOption) {
                                        perDay -> (goalAmount.toDoubleOrNull() ?: 0.0) / (goalPeriod * 30)
                                        perWeek -> (goalAmount.toDoubleOrNull() ?: 0.0) / (goalPeriod * 4)
                                        perMonth -> (goalAmount.toDoubleOrNull() ?: 0.0) / goalPeriod
                                        perYear -> (goalAmount.toDoubleOrNull() ?: 0.0) / (goalPeriod / 12)
                                        else -> 0.0
                                    }
                                    // Save the selected frequency and calculated saving to SharedPreferences
                                    with(sharedPreferences.edit()) {
                                        putString("saving_frequency", savingFrequency)
                                        putFloat("calculated_saving", calculatedSaving.toFloat())
                                        apply()
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(id = R.string.total_savings)}: ${savedAmounts.sum()} $selectedCurrency",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewSavings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(stringResource(id = R.string.view_savings), color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = savedAmount,
                    onValueChange = onSavedAmountChange,
                    label = { Text(stringResource(id = R.string.add_saving), color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DatePickerField(
                    selectedDate = selectedDate,
                    onDateChange = { selectedDate = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val newSavedAmount = savedAmount.toDoubleOrNull()
                        if (newSavedAmount != null && newSavedAmount > 0) {
                            onAddSaving()
                            val sharedPreferences = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            val updatedSavedAmounts = savedAmounts + newSavedAmount
                            editor.putString("saved_amounts", Gson().toJson(updatedSavedAmounts))
                            editor.apply()
                            Toast.makeText(localContext, "${localContext.getString(R.string.you_saved)} $newSavedAmount $selectedCurrency", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(localContext, localContext.getString(R.string.enter_valid_value), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400).copy(alpha = 0.4f)), // Темно-зелена прозора кнопка
                    shape = RoundedCornerShape(0.dp), // Прямі кути
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(id = R.string.ok), color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween // Розташування кнопок поряд
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .width(120.dp) // Додана ширина кнопок
                            .height(40.dp) // Висота кнопок
                    ) {
                        Text(stringResource(id = R.string.cancel), color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onSaveGoal,
                        modifier = Modifier
                            .width(120.dp) // Додана ширина кнопок
                            .height(40.dp) // Висота кнопок
                    ) {
                        Text(stringResource(id = R.string.save), color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color.Transparent // Прозорість до самого меню
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(selectedDate: String, onDateChange: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = remember {
        DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            onDateChange(formattedDate)
        }, year, month, day)
    }

    OutlinedTextField(
        value = selectedDate,
        onValueChange = { },
        label = { Text(stringResource(id = R.string.select_date), color = Color.Gray) },
        readOnly = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold),
        trailingIcon = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color.White)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ProgressChart(percentageToGoal: Double, modifier: Modifier = Modifier, strokeWidth: Dp = 8.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = modifier) {
            val progressAngle = (percentageToGoal / 100f) * 360f
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx())
            )
            drawArc(
                color = Color.Green,
                startAngle = -90f,
                sweepAngle = progressAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx())
            )
        }
        Text(
            text = "${percentageToGoal.format(2)}%",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            fontSize = 16.sp,
        )
    }
}

// Додано: функція для форматування відсотка
fun Double.format(digits: Int) = "%.${digits}f".format(this)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsListDialog(
    savingsList: List<Pair<Double, String>>,
    onDismissRequest: () -> Unit,
    onUpdateSaving: (Int, Double) -> Unit,
    onDeleteSaving: (Int) -> Unit,
    selectedCurrency: String // Add selectedCurrency parameter
) {
    var editingIndex by remember { mutableStateOf(-1) }
    var editedAmount by remember { mutableStateOf("") }

    if (editingIndex != -1) {
        Dialog(onDismissRequest = { editingIndex = -1 }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.edit_saving),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedAmount,
                        onValueChange = { editedAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(id = R.string.saving_amount), color = Color.Gray) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { editingIndex = -1 }) {
                            Text(stringResource(id = R.string.cancel), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val newAmount = editedAmount.toDoubleOrNull()
                            if (newAmount != null) {
                                onUpdateSaving(editingIndex, newAmount)
                                editingIndex = -1
                            }
                        }) {
                            Text(stringResource(id = R.string.save), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.Black.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.savings_list),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(savingsList) { index, (amount, date) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$amount $selectedCurrency",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = date,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Row {
                                IconButton(onClick = {
                                    editedAmount = amount.toString()
                                    editingIndex = index
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = Color.White)
                                }
                                IconButton(onClick = { onDeleteSaving(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(id = R.string.close), color = Color.White)
                    }
                }
            }
        }
    }
}

fun String.formatWithSpaces(): String {
    return this.reversed().chunked(3).joinToString(" ").reversed()
}
