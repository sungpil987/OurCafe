package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

private fun money13(n: Long): String = NumberFormat.getNumberInstance(Locale.KOREA).format(n) + "원"

@Composable
fun MoneyDashboard13() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_local", 0) }
    var target by remember { mutableStateOf(prefs.getString("money13_target", "") ?: "") }
    var rent by remember { mutableStateOf(prefs.getString("money13_rent", "") ?: "") }
    var labor by remember { mutableStateOf(prefs.getString("money13_labor", "") ?: "") }
    var utilities by remember { mutableStateOf(prefs.getString("money13_utilities", "") ?: "") }
    var other by remember { mutableStateOf(prefs.getString("money13_other", "") ?: "") }
    var salePrice by remember { mutableStateOf(prefs.getString("money13_sale", "") ?: "") }
    var unitCost by remember { mutableStateOf(prefs.getString("money13_cost", "") ?: "") }
    var days by remember { mutableStateOf(prefs.getString("money13_days", "26") ?: "26") }
    var refresh by remember { mutableIntStateOf(0) }
    val raw = remember(refresh) { prefs.getString("expenses", "[]") ?: "[]" }
    val expenses = remember(raw) {
        runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.optString(it) }
        }.getOrDefault(emptyList())
    }
    val spent = expenses.sumOf { it.substringBefore("|").toLongOrNull() ?: 0L }
    val budget = target.toLongOrNull() ?: 0L
    val fixed = listOf(rent, labor, utilities, other).sumOf { it.toLongOrNull() ?: 0L }
    val margin = (salePrice.toLongOrNull() ?: 0L) - (unitCost.toLongOrNull() ?: 0L)
    val required = if (fixed > 0 && margin > 0) ceil(fixed.toDouble() / margin).toLong() else null
    val perDay = if (required != null && (days.toIntOrNull() ?: 0) > 0)
        ceil(required.toDouble() / (days.toIntOrNull() ?: 1)).toLong() else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MONEY · 창업 재무 대시보드", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("기존 예산 기록을 읽어 분석합니다. 기록을 이동하거나 삭제하지 않아요.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = target, onValueChange = { target = it.filter(Char::isDigit) },
            label = { Text("창업 총예산 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("창업 예산 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("현재 지출: ${money13(spent)}")
                Text("예산 잔액: " + if (budget > 0) money13(budget - spent) else "예산을 입력해 주세요")
                if (budget > 0) {
                    LinearProgressIndicator(progress = { (spent.toFloat() / budget).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    if (spent > budget) Text("예산을 ${money13(spent - budget)} 초과했어요.", color = MaterialTheme.colorScheme.error)
                }
                Text("지출 기록 ${expenses.size}건", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("월 고정비 계획", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        listOf("월세" to rent, "인건비" to labor, "공과금" to utilities, "기타 고정비" to other).forEach { (label, value) ->
            OutlinedTextField(value = value, onValueChange = { v ->
                val clean = v.filter(Char::isDigit)
                when(label) {
                    "월세" -> rent = clean
                    "인건비" -> labor = clean
                    "공과금" -> utilities = clean
                    else -> other = clean
                }
            }, label = { Text("$label (월/원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
        Text("월 고정비 합계: ${money13(fixed)}", fontWeight = FontWeight.Bold)
        Text("손익분기점 시뮬레이션", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("대표 메뉴 한 가지의 판매가와 변동원가를 기준으로 단순 추정합니다.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = salePrice, onValueChange = { salePrice = it.filter(Char::isDigit) },
            label = { Text("대표 메뉴 판매가 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = unitCost, onValueChange = { unitCost = it.filter(Char::isDigit) },
            label = { Text("잔당 변동원가 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = days, onValueChange = { days = it.filter(Char::isDigit) },
            label = { Text("월 영업일수") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("잔당 공헌이익: ${money13(margin)}", fontWeight = FontWeight.Bold)
                Text(if (required == null) "월 고정비와 양수의 잔당 공헌이익을 입력하세요." else "월 손익분기 판매량: ${required}잔")
                if (perDay != null) Text("하루 평균 필요 판매량: ${perDay}잔")
                Text("세금, 결제 수수료, 폐기, 제품별 판매 비중 등은 별도 반영이 필요합니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            prefs.edit().putString("money13_target", target).putString("money13_rent", rent)
                .putString("money13_labor", labor).putString("money13_utilities", utilities)
                .putString("money13_other", other).putString("money13_sale", salePrice)
                .putString("money13_cost", unitCost).putString("money13_days", days).apply()
        }) { Text("재무 계획 저장") }
        OutlinedButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("최신 지출 기록 새로고침") }
        Text("이 화면의 재무 계획은 현재 휴대폰에 저장됩니다. 공동 공간과 자동 동기화되지 않습니다.", style = MaterialTheme.typography.bodySmall)
    }
}
