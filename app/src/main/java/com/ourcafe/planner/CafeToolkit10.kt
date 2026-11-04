package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private fun load10(raw: String?): List<JSONObject> = try {
    val arr = JSONArray(raw ?: "[]")
    (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
} catch (_: Exception) { emptyList() }
private fun format10(v: Long): String = NumberFormat.getNumberInstance(Locale.KOREA).format(v) + "원"
private fun num10(raw: String): Long = raw.toLongOrNull() ?: 0L

@Composable
private fun Entry10(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (numeric) it.filter(Char::isDigit) else it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        singleLine = true
    )
}

@Composable
private fun Records10(rows: List<JSONObject>, title: (JSONObject) -> String, detail: (JSONObject) -> String, remove: (JSONObject) -> Unit) {
    if (rows.isEmpty()) Text("아직 기록이 없어요. 첫 항목을 추가해 보세요.", style = MaterialTheme.typography.bodySmall)
    rows.asReversed().forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title(item), fontWeight = FontWeight.Bold)
                    Text(detail(item), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { remove(item) }) { Icon(Icons.Outlined.Delete, "삭제") }
            }
        }
    }
}

@Composable
fun CompetitorResearch10() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_10", 0)
    var rows by remember { mutableStateOf(load10(prefs.getString("competitors", null))) }
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var strengths by remember { mutableStateOf("") }
    var weakness by remember { mutableStateOf("") }
    var traffic by remember { mutableStateOf("") }
    Text("경쟁 카페 현장 조사", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("경쟁 카페를 직접 방문해서 가격·장점·아쉬운 점·혼잡도를 비교해요.")
    Entry10("카페 이름", name) { name = it }
    Entry10("지역·위치", area) { area = it }
    Entry10("대표 음료 가격 (원)", price, true) { price = it }
    Entry10("강점 · 다시 방문할 이유", strengths) { strengths = it }
    Entry10("아쉬운 점 · 차별화 기회", weakness) { weakness = it }
    Entry10("방문 요일·시간대·손님 관찰", traffic) { traffic = it }
    Button(enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("area", area.trim()).put("price", num10(price)).put("strengths", strengths.trim()).put("weakness", weakness.trim()).put("traffic", traffic.trim())
        prefs.edit().putString("competitors", JSONArray(rows).toString()).apply()
        name = ""; area = ""; price = ""; strengths = ""; weakness = ""; traffic = ""
    }) { Text("경쟁 카페 기록 저장") }
    Records10(rows,
        { it.optString("name") + " · " + format10(it.optLong("price")) },
        { "위치: ${it.optString("area")}\n강점: ${it.optString("strengths")}\n약점: ${it.optString("weakness")}\n현장: ${it.optString("traffic")}" },
        { item -> rows = rows.filterNot { it === item }; prefs.edit().putString("competitors", JSONArray(rows).toString()).apply() }
    )
    Text("관찰은 여러 요일·시간대에 반복하세요. 단일 방문으로 매출을 추정하지 마세요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun VendorQuotes10() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_10", 0)
    var rows by remember { mutableStateOf(load10(prefs.getString("quotes", null))) }
    var item by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var warranty by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Text("장비·공사 견적 비교", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("같은 품목의 업체별 가격, 설치·배송 조건, AS를 비교해요.")
    Entry10("품목 (예: 에스프레소 머신)", item) { item = it }
    Entry10("업체·브랜드", vendor) { vendor = it }
    Entry10("견적 총액 (원)", cost, true) { cost = it }
    Entry10("설치·배송·AS·보증 조건", warranty) { warranty = it }
    Entry10("비고 / 견적 받은 날짜", note) { note = it }
    Button(enabled = item.isNotBlank() && vendor.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), onClick = {
        rows = rows + JSONObject().put("item", item.trim()).put("vendor", vendor.trim()).put("cost", num10(cost)).put("warranty", warranty.trim()).put("note", note.trim())
        prefs.edit().putString("quotes", JSONArray(rows).toString()).apply()
        item = ""; vendor = ""; cost = ""; warranty = ""; note = ""
    }) { Text("견적 저장") }
    val grouped = rows.groupBy { it.optString("item") }
    grouped.forEach { (category, entries) ->
        Spacer(Modifier.height(12.dp))
        Text(category + " · " + entries.size + "개 견적", fontWeight = FontWeight.Bold)
        Records10(entries.sortedBy { it.optLong("cost") },
            { it.optString("vendor") + " · " + format10(it.optLong("cost")) },
            { "설치/AS: ${it.optString("warranty")}\n비고: ${it.optString("note")}" },
            { target -> rows = rows.filterNot { it === target }; prefs.edit().putString("quotes", JSONArray(rows).toString()).apply() }
        )
    }
    Text("가격이 낮아도 부가세·설치비·수리 조건이 다를 수 있으니 동일한 범위로 비교하세요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun MenuProfit10() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_10", 0)
    var rows by remember { mutableStateOf(load10(prefs.getString("menu_profit", null))) }
    var name by remember { mutableStateOf("") }
    var sale by remember { mutableStateOf("") }
    var ingredient by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var waste by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val revenue = num10(sale)
    val cost = num10(ingredient) + num10(packaging) + num10(fee) + num10(waste)
    val margin = revenue - cost
    Text("메뉴별 기여이익 계산", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("재료비 외에 컵·포장·결제 수수료·예상 폐기비까지 넣어 실제에 가까운 주문당 이익을 계산해요.")
    Entry10("메뉴명", name) { name = it }
    Entry10("판매가 (원)", sale, true) { sale = it }
    Entry10("재료 원가 (원)", ingredient, true) { ingredient = it }
    Entry10("컵·포장 원가 (원)", packaging, true) { packaging = it }
    Entry10("결제·플랫폼 수수료 (원)", fee, true) { fee = it }
    Entry10("폐기·손실 배분액 (원)", waste, true) { waste = it }
    Entry10("제조시간 (분)", time, true) { time = it }
    Card(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("주문당 예상 기여이익", fontWeight = FontWeight.Bold)
            Text(format10(margin), style = MaterialTheme.typography.headlineSmall, color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text("기여이익률: ${if (revenue > 0) margin * 100 / revenue else 0}% · 변동비: ${format10(cost)}")
        }
    }
    Button(enabled = name.isNotBlank() && revenue > 0, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("sale", revenue).put("cost", cost).put("margin", margin).put("time", num10(time))
        prefs.edit().putString("menu_profit", JSONArray(rows).toString()).apply()
        name = ""; sale = ""; ingredient = ""; packaging = ""; fee = ""; waste = ""; time = ""
    }) { Text("메뉴 계산 결과 저장") }
    Records10(rows,
        { it.optString("name") + " · 기여이익 " + format10(it.optLong("margin")) },
        { "판매가 ${format10(it.optLong("sale"))} / 변동비 ${format10(it.optLong("cost"))} / 제조 ${it.optLong("time")}분" },
        { target -> rows = rows.filterNot { it === target }; prefs.edit().putString("menu_profit", JSONArray(rows).toString()).apply() }
    )
    Text("기여이익은 순이익이 아닙니다. 인건비·월세·세금·감가상각 등은 별도 계산해야 해요.", style = MaterialTheme.typography.bodySmall)
}

private val launch10 = listOf(
    "사업자등록·영업신고 등 해당 절차 확인",
    "위생교육·건강진단 등 해당 의무 확인",
    "소방·가스·전기·급배수 시설 점검",
    "원재료·포장재·소모품 첫 발주",
    "메뉴 가격표·알레르기 등 표시사항 확인",
    "레시피와 제조 순서 테스트",
    "POS·카드결제·현금영수증 테스트",
    "지도 등록·영업시간·연락처 확인",
    "직원 역할·응대·위생·마감 교육",
    "피크타임 주문 흐름 리허설",
    "냉장·냉동 보관 온도 점검",
    "청소·폐기물·방역 운영 계획",
    "가오픈 고객 의견 수집",
    "비상 연락처·AS·거래처 연락망 정리",
    "오픈 첫 주 매출·재고·피드백 점검"
)
@Composable
fun LaunchReady10() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_10", 0)
    var checked by remember { mutableStateOf(prefs.getStringSet("launch", emptySet())?.toSet() ?: emptySet()) }
    var note by remember { mutableStateOf(prefs.getString("launch_note", "") ?: "") }
    Text("가오픈·오픈 준비 센터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("오픈 직전 실무 점검 15가지. 실제 업종과 시설에 맞춰 필요한 항목을 추가 확인하세요.")
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(progress = { checked.size.toFloat() / launch10.size }, modifier = Modifier.fillMaxWidth())
    Text("${checked.size} / ${launch10.size}개 완료", fontWeight = FontWeight.Bold)
    launch10.forEach { item ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item in checked, onCheckedChange = { yes ->
                checked = if (yes) checked + item else checked - item
                prefs.edit().putStringSet("launch", checked).apply()
            })
            Text(item, modifier = Modifier.weight(1f))
        }
    }
    OutlinedTextField(note, { note = it; prefs.edit().putString("launch_note", note).apply() },
        label = { Text("오픈 전 미해결 문제·담당자·기한") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Text("체크리스트는 법적 인허가 확인을 대체하지 않습니다.", style = MaterialTheme.typography.bodySmall)
}
