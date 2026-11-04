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

private fun won11(n: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(n) + "원"
private fun records11(s: String?): List<JSONObject> = try { val a = JSONArray(s ?: "[]"); (0 until a.length()).mapNotNull { a.optJSONObject(it) } } catch (_: Exception) { emptyList() }
private fun number11(s: String) = s.toLongOrNull() ?: 0L

@Composable private fun Field11(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(if (numeric) it.filter(Char::isDigit) else it) }, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
}
@Composable private fun Item11(title: String, subtitle: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "삭제") }
        }
    }
}
@Composable
fun Inventory11() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_11", 0)
    var rows by remember { mutableStateOf(records11(prefs.getString("inventory", null))) }
    var name by remember { mutableStateOf("") }; var unit by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }; var min by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    Text("원재료·재고 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("최소 재고보다 적으면 발주가 필요한 품목으로 표시돼요. 수량 단위는 품목별로 통일하세요.")
    Field11("원재료명 (예: 우유)", name) { name = it }
    Field11("단위 (예: L, kg, 개)", unit) { unit = it }
    Field11("현재 수량", qty, true) { qty = it }
    Field11("최소 유지 수량", min, true) { min = it }
    Field11("유통기한·보관 메모", expiry) { expiry = it }
    Button(enabled = name.isNotBlank() && unit.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("unit", unit.trim()).put("qty", number11(qty)).put("min", number11(min)).put("expiry", expiry.trim())
        prefs.edit().putString("inventory", JSONArray(rows).toString()).apply()
        name = ""; unit = ""; qty = ""; min = ""; expiry = ""
    }) { Text("재고 품목 저장") }
    val low = rows.count { it.optLong("qty") <= it.optLong("min") }
    Text("관리 품목 ${rows.size}개 · 발주 확인 필요 $low개", fontWeight = FontWeight.Bold)
    rows.forEach { item ->
        val lowStock = item.optLong("qty") <= item.optLong("min")
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.optString("name") + if (lowStock) " · 발주 확인" else "", fontWeight = FontWeight.Bold,
                            color = if (lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        Text("현재 ${item.optLong("qty")} ${item.optString("unit")} / 최소 ${item.optLong("min")} ${item.optString("unit")}")
                        if (item.optString("expiry").isNotBlank()) Text(item.optString("expiry"), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { rows = rows.filterNot { it === item }; prefs.edit().putString("inventory", JSONArray(rows).toString()).apply() }) {
                        Icon(Icons.Outlined.Delete, "삭제")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val next = item.optLong("qty").minus(1).coerceAtLeast(0)
                        rows = rows.map { if (it === item) JSONObject(it.toString()).put("qty", next) else it }
                        prefs.edit().putString("inventory", JSONArray(rows).toString()).apply()
                    }) { Text("− 1") }
                    OutlinedButton(onClick = {
                        rows = rows.map { if (it === item) JSONObject(it.toString()).put("qty", item.optLong("qty") + 1) else it }
                        prefs.edit().putString("inventory", JSONArray(rows).toString()).apply()
                    }) { Text("+ 1") }
                }
            }
        }
    }
    Text("수량은 수동으로 조정해야 하며 실제 판매·폐기와 자동 연동되지 않아요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun Vendors11() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_11", 0)
    var rows by remember { mutableStateOf(records11(prefs.getString("vendors", null))) }
    var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }; var lead by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    Text("거래처·발주 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("원두, 우유, 베이커리, 포장재 업체의 연락처와 발주 조건을 모아두세요.")
    Field11("거래처 이름", name) { name = it }
    Field11("납품 품목", category) { category = it }
    Field11("연락처", phone) { phone = it }
    Field11("발주 마감·배송 소요일", lead) { lead = it }
    Field11("최소 주문량·배송비·결제 조건", memo) { memo = it }
    Button(enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("category", category.trim()).put("phone", phone.trim()).put("lead", lead.trim()).put("memo", memo.trim())
        prefs.edit().putString("vendors", JSONArray(rows).toString()).apply()
        name = ""; category = ""; phone = ""; lead = ""; memo = ""
    }) { Text("거래처 저장") }
    rows.forEach { item -> Item11(item.optString("name"), "${item.optString("category")} · ${item.optString("phone")}\n발주/배송: ${item.optString("lead")}\n조건: ${item.optString("memo")}") {
        rows = rows.filterNot { it === item }; prefs.edit().putString("vendors", JSONArray(rows).toString()).apply()
    } }
    Text("실제 주문 전송 기능은 없어요. 업체와 발주 조건을 다시 확인하세요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun Labor11() {
    var workers by remember { mutableFloatStateOf(2f) }
    var hours by remember { mutableFloatStateOf(6f) }
    var days by remember { mutableFloatStateOf(22f) }
    var hourly by remember { mutableStateOf("11000") }
    val base = workers.toLong() * hours.toLong() * days.toLong() * number11(hourly)
    Text("근무·인건비 계산기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("직원 수와 근무시간으로 기본 시급 총액을 빠르게 계산해요.")
    Field11("시간당 급여 (원)", hourly, true) { hourly = it }
    Text("근무 직원 수: ${workers.toInt()}명")
    Slider(workers, { workers = it }, valueRange = 1f..12f, steps = 10)
    Text("하루 근무시간: ${hours.toInt()}시간")
    Slider(hours, { hours = it }, valueRange = 1f..12f, steps = 10)
    Text("월 근무일: ${days.toInt()}일")
    Slider(days, { days = it }, valueRange = 1f..31f, steps = 29)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("월 기본 시급 총액", fontWeight = FontWeight.Bold)
            Text(won11(base), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
    Text("주휴수당·연장·야간·휴일수당·4대 보험·퇴직급여·세금·휴게시간 등은 미반영입니다. 실제 급여와 법정 최저임금은 최신 기준으로 별도 확인하세요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun Seats11() {
    var seats by remember { mutableFloatStateOf(16f) }
    var turns by remember { mutableFloatStateOf(3f) }
    var ticket by remember { mutableStateOf("6500") }
    var days by remember { mutableFloatStateOf(26f) }
    var occupancy by remember { mutableFloatStateOf(60f) }
    val visits = seats.toDouble() * turns.toDouble() * occupancy.toDouble() / 100
    val daily = (visits * number11(ticket)).toLong()
    Text("좌석·회전율 매출 시뮬레이터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("좌석 중심 카페에서 이용률과 회전율에 따른 매출을 가정해요.")
    Text("좌석 수: ${seats.toInt()}석")
    Slider(seats, { seats = it }, valueRange = 2f..100f)
    Text("하루 좌석 회전 횟수: ${turns.toInt()}회")
    Slider(turns, { turns = it }, valueRange = 1f..10f, steps = 8)
    Text("좌석 이용률: ${occupancy.toInt()}%")
    Slider(occupancy, { occupancy = it }, valueRange = 0f..100f)
    Text("월 영업일: ${days.toInt()}일")
    Slider(days, { days = it }, valueRange = 15f..31f)
    Field11("고객 1인당 평균 매출 (원)", ticket, true) { ticket = it }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("예상 하루 방문 고객: 약 ${visits.toInt()}명")
            Text("예상 일매출: ${won11(daily)}")
            Text("예상 월매출: ${won11((daily * days).toLong())}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
    Text("테이크아웃·배달 매출은 제외한 가상 계산이며 실제 방문 수요나 이익을 보장하지 않습니다.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun Recipes11() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_11", 0)
    var rows by remember { mutableStateOf(records11(prefs.getString("recipes", null))) }
    var name by remember { mutableStateOf("") }; var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }; var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    Text("표준 레시피 카드", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("누가 제조해도 같은 맛이 나오도록 계량과 순서를 구체적으로 적어두세요.")
    Field11("메뉴 이름", name) { name = it }
    OutlinedTextField(ingredients, { ingredients = it }, label = { Text("재료와 정확한 계량 (g, ml, 샷)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(steps, { steps = it }, label = { Text("제조 순서 / 온도 / 추출 시간") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Field11("제조시간 (분)", time, true) { time = it }
    Field11("보관·알레르기·품질 주의사항", notes) { notes = it }
    Button(enabled = name.isNotBlank() && steps.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("ingredients", ingredients.trim()).put("steps", steps.trim()).put("time", time.trim()).put("notes", notes.trim())
        prefs.edit().putString("recipes", JSONArray(rows).toString()).apply()
        name = ""; ingredients = ""; steps = ""; time = ""; notes = ""
    }) { Text("레시피 저장") }
    rows.forEach { item -> Item11(item.optString("name"), "재료: ${item.optString("ingredients")}\n순서: ${item.optString("steps")}\n제조시간: ${item.optString("time")}분\n주의: ${item.optString("notes")}") {
        rows = rows.filterNot { it === item }; prefs.edit().putString("recipes", JSONArray(rows).toString()).apply()
    } }
}

@Composable
fun Branding11() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_11", 0)
    val fields = listOf("브랜드 이름", "한 줄 슬로건", "주 고객층", "우리 카페의 차별점", "공간 분위기·색상", "대표 메뉴와 가격대", "고객이 다시 방문할 이유", "브랜드 운영 원칙")
    Text("브랜드 콘셉트 노트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("정리한 내용은 자동 저장돼요. 두 사람이 창업 방향을 논의할 때 기준으로 활용하세요.")
    fields.forEachIndexed { index, label ->
        var value by remember(index) { mutableStateOf(prefs.getString("brand_$index", "") ?: "") }
        OutlinedTextField(value, { value = it; prefs.edit().putString("brand_$index", it).apply() }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), minLines = if (index > 2) 2 else 1)
    }
    Text("상호·상표 사용 가능 여부는 별도로 확인해야 합니다.", style = MaterialTheme.typography.bodySmall)
}

private val marketing11 = listOf("브랜드 이름·로고 확정", "메뉴 사진·가격표 촬영", "네이버 지도 등 매장 정보 등록 준비", "SNS 프로필·소개·운영시간 작성", "오픈 전 공사·준비 과정 콘텐츠 제작", "가오픈 대상과 일정 확정", "고객 안내문·주차·찾아오는 길 점검", "첫 방문 고객 피드백 수집 계획", "리뷰 응대 원칙 정리", "오픈 후 2주 홍보 성과 점검")
@Composable
fun Marketing11() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_11", 0)
    var checked by remember { mutableStateOf(prefs.getStringSet("marketing", emptySet())?.toSet() ?: emptySet()) }
    var memo by remember { mutableStateOf(prefs.getString("marketing_memo", "") ?: "") }
    Text("오픈 홍보·마케팅 계획", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("오픈 전후 홍보에 필요한 10가지 준비 항목을 확인해요.")
    LinearProgressIndicator(progress = { checked.size.toFloat() / marketing11.size }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    Text("${checked.size} / ${marketing11.size} 완료")
    marketing11.forEach { item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item in checked, onCheckedChange = { yes ->
                checked = if (yes) checked + item else checked - item
                prefs.edit().putStringSet("marketing", checked).apply()
            })
            Text(item, modifier = Modifier.weight(1f))
        }
    }
    OutlinedTextField(memo, { memo = it; prefs.edit().putString("marketing_memo", it).apply() }, label = { Text("채널별 홍보 일정·예산·아이디어") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Text("홍보 성과는 방문·재방문·실제 주문 등으로 검증하세요.", style = MaterialTheme.typography.bodySmall)
}
