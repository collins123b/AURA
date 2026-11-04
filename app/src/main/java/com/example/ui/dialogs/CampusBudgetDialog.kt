package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AcademicExpenseEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CampusBudgetDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val expenses by viewModel.academicExpenses.collectAsState()

    var showAddExpense by remember { mutableStateOf(false) }
    var showEditBudget by remember { mutableStateOf(false) }

    val totalSpent = remember(expenses) { expenses.sumOf { it.amount } }
    val remaining = (profile.semesterBudgetLimit - totalSpent)
    val spentRatio = if (profile.semesterBudgetLimit > 0.0) {
        (totalSpent / profile.semesterBudgetLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val categories = listOf(
        "PRINTING" to "Printing & Handouts",
        "TEXTBOOKS" to "Books & Lab Manuals",
        "TRANSPORT" to "Campus Transport",
        "FEEDING" to "Campus Meals",
        "DATA" to "Data & Internet",
        "OTHER" to "Other Academic"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LocalAuraColors.current.warning.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = LocalAuraColors.current.warning,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Semester Budget & Expenses",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Track printing, handouts, transport & textbooks",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LocalAuraColors.current.textMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("campus_budget_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Card
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Remaining Semester Balance",
                                    fontSize = 11.sp,
                                    color = LocalAuraColors.current.textSecondary
                                )
                                Text(
                                    text = "${profile.currencySymbol} ${String.format(Locale.US, "%.2f", remaining)}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) LocalAuraColors.current.success else LocalAuraColors.current.danger
                                )
                            }
                            IconButton(onClick = { showEditBudget = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Budget Limit",
                                    tint = LocalAuraColors.current.secondaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { spentRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                spentRatio > 0.9f -> LocalAuraColors.current.danger
                                spentRatio > 0.7f -> LocalAuraColors.current.warning
                                else -> LocalAuraColors.current.primaryAccent
                            },
                            trackColor = Color(0x22FFFFFF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Spent ${profile.currencySymbol} ${String.format(Locale.US, "%.2f", totalSpent)} of ${profile.currencySymbol} ${String.format(Locale.US, "%.2f", profile.semesterBudgetLimit)} • Safe 30-day allowance: ${profile.currencySymbol} ${String.format(Locale.US, "%.1f", (remaining.coerceAtLeast(0.0) / 30.0))}/day",
                            fontSize = 11.sp,
                            color = LocalAuraColors.current.textSecondary
                        )
                    }
                }

                // Category Breakdown
                if (expenses.isNotEmpty()) {
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category Breakdown",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            categories.forEach { (key, label) ->
                                val catSum = expenses.filter { it.category == key }.sumOf { it.amount }
                                if (catSum > 0.0) {
                                    val ratio = (catSum / totalSpent.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(label, fontSize = 11.sp, color = LocalAuraColors.current.textSecondary, modifier = Modifier.weight(1f))
                                            Text(
                                                "${profile.currencySymbol} ${String.format(Locale.US, "%.2f", catSum)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LocalAuraColors.current.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = LocalAuraColors.current.secondaryAccent,
                                            trackColor = Color(0x1EFFFFFF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Expenses (${expenses.size})",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    AuraGlassButton(
                        text = "Log Expense",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = { showAddExpense = true },
                        testTag = "add_academic_expense_button"
                    )
                }

                if (expenses.isEmpty()) {
                    Text(
                        text = "No academic expenses logged yet. Tap 'Log Expense' to track printing, textbooks, or transport.",
                        fontSize = 12.sp,
                        color = LocalAuraColors.current.textMuted
                    )
                } else {
                    val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
                    expenses.take(15).forEach { exp ->
                        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(exp.dateEpochMillis), ZoneId.systemDefault())
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AuraBadge(text = exp.category, textColor = LocalAuraColors.current.warning)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(dt.format(fmt), fontSize = 10.sp, color = LocalAuraColors.current.textMuted)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(exp.title, fontWeight = FontWeight.SemiBold, color = LocalAuraColors.current.textPrimary, fontSize = 13.sp)
                                }
                                Text(
                                    text = "${profile.currencySymbol} ${String.format(Locale.US, "%.2f", exp.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = { viewModel.deleteAcademicExpense(exp) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Expense", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(text = "Done", onClick = onDismiss, testTag = "close_budget_dialog_button")
        },
        containerColor = Color(0xF0101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
            RoundedCornerShape(24.dp)
        )
    )

    if (showAddExpense) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("PRINTING") }

        AlertDialog(
            onDismissRequest = { showAddExpense = false },
            title = { Text("Log Academic Expense", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item (e.g. Lab Manual Printing)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (${profile.currencySymbol})") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Category:", fontSize = 11.sp, color = LocalAuraColors.current.textSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { (key, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (category == key) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF))
                                            .clickable { category = key }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Expense",
                    isPrimary = true,
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (title.isNotBlank() && amt != null && amt > 0.0) {
                            viewModel.addAcademicExpense(
                                AcademicExpenseEntity(
                                    title = title.trim(),
                                    category = category,
                                    amount = amt
                                )
                            )
                            showAddExpense = false
                        }
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(text = "Cancel", onClick = { showAddExpense = false })
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showEditBudget) {
        var limitText by remember { mutableStateOf(profile.semesterBudgetLimit.toString()) }
        var currencyText by remember { mutableStateOf(profile.currencySymbol) }

        AlertDialog(
            onDismissRequest = { showEditBudget = false },
            title = { Text("Set Semester Budget", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = currencyText, onValueChange = { currencyText = it }, label = { Text("Currency Symbol (e.g. GH₵, $, €, ₦)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Semester / Monthly Budget") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Update Budget",
                    isPrimary = true,
                    onClick = {
                        val parsed = limitText.toDoubleOrNull() ?: profile.semesterBudgetLimit
                        viewModel.updateBudgetSettings(parsed, currencyText.trim())
                        showEditBudget = false
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(text = "Cancel", onClick = { showEditBudget = false })
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(22.dp)
        )
    }
}
