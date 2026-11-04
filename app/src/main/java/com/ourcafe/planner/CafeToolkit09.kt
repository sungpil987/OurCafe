package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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

private fun money09(n: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(n) + "원"
private fun load09(s: String?): List<JSONObject> = try { val a = JSONArray(s ?: "[]"); (0 until a.length()).mapNotNull { a.optJSONObject(it) } } catch (_: Exception) { emptyList() }
private fun persist09(rows: List<JSONObject>) = JSONArray(rows).toString()

private val criteria09 = listOf(
    "목표 고객과 상권 일치" to 20, "실제 방문 수요" to 20, "임대료 대비 예상 매출" to 20,
    "접근성·주차·가시성" to 15, "차별화 가능성" to 10, "건물·설비·인허가" to 10, "향후 상권 변화" to 5
)
@Composable
fun PropertyScorer09() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("ourcafe_09", 0) }
    var rows by remember { mutableStateOf(load09(prefs.getString("scores", null))) }
    var name by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val values = remember { mutableStateListOf(*Array(criteria09.size) { 0f }) }
    val total = criteria09.indices.sumOf { (values[it] * criteria09[it].second).toInt() }
    Text("매물별 상권 평가", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("각 항목을 0~5점으로 평가하면 가중치를 반영해 100점 만점으로 비교해요. 실제 조사 전에는 0점으로 두세요.")
    OutlinedTextField(name, { name = it }, label = { Text("후보 매물 이름") }, modifier = Modifier.fillMaxWidth())
    criteria09.forEachIndexed { index, (label, weight) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${values[index].toInt()}/5 · ${(values[index] * weight / 5).toInt()}/$weight")
        }
        Slider(value = values[index], onValueChange = { values[index] = it }, valueRange = 0f..5f, steps = 4)
    }
    val weighted = criteria09.indices.sumOf { (values[it] * criteria09[it].second / 5).toInt() }
    Text("총점 $weighted / 100", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    OutlinedTextField(memo, { memo = it }, label = { Text("현장 조사 근거 / 위험 요인") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    Button(enabled = name.isNotBlank(), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("score", weighted).put("memo", memo.trim())
        prefs.edit().putString("scores", persist09(rows)).apply()
        name = ""; memo = ""; criteria09.indices.forEach { values[it] = 0f }
    }, modifier = Modifier.fillMaxWidth()) { Text("평가 결과 저장") }
    rows.sortedByDescending { it.optInt("score") }.forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${item.optString("name")} · ${item.optInt("score")}점", fontWeight = FontWeight.Bold)
                    if (item.optString("memo").isNotBlank()) Text(item.optString("memo"), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { rows = rows.filterNot { it === item }; prefs.edit().putString("scores", persist09(rows)).apply() }) { Icon(Icons.Outlined.Delete, "평가 삭제") }
            }
        }
    }
    Text("이 점수는 직접 정한 주관적 평가입니다. 영업 불가 건물·예산 초과·법적 위험은 점수와 무관하게 별도 검토하세요.", style = MaterialTheme.typography.bodySmall)
}

private val contract09 = listOf(
    "건축물대장 용도와 위반건축물 여부 확인",
    "관할 지자체에 해당 영업 형태의 신고 가능 여부 확인",
    "등기부등본 소유자·근저당·권리관계 확인",
    "임대인과 계약 상대방 일치 여부 확인",
    "보증금·월세·관리비·부가세 조건 명시",
    "권리금·시설 인수 범위·영업 양도 조건 검토",
    "전기 용량·급배수·환기·가스·소방 시설 확인",
    "간판·외부 공간·테라스 설치 권한 확인",
    "공사 기간 무상 임대·인테리어 공사 동의 확인",
    "원상복구 범위·하자 책임·중도 해지 조건 확인",
    "필요 인허가 불가능 시 처리 조건 전문가 검토",
    "계약금 송금 전 최종 서류 및 현장 재확인"
)
@Composable
fun ContractChecklist09() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("ourcafe_09", 0) }
    var checked by remember { mutableStateOf(prefs.getStringSet("contract_checked", emptySet())?.toSet() ?: emptySet()) }
    var notes by remember { mutableStateOf(prefs.getString("contract_notes", "") ?: "") }
    Text("임대차 계약 전 필수 점검", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("계약금을 보내기 전에 하나씩 확인하세요. 체크는 법률 검토 완료를 의미하지 않아요.")
    Spacer(Modifier.height(10.dp))
    LinearProgressIndicator(progress = { checked.size.toFloat() / contract09.size }, modifier = Modifier.fillMaxWidth())
    Text("${checked.size} / ${contract09.size}개 확인")
    contract09.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Checkbox(checked = item in checked, onCheckedChange = {
                checked = if (it) checked + item else checked - item
                prefs.edit().putStringSet("contract_checked", checked).apply()
            })
            Text(item, modifier = Modifier.weight(1f))
        }
    }
    OutlinedTextField(notes, { notes = it; prefs.edit().putString("contract_notes", notes).apply() },
        label = { Text("중개사 질문 / 계약 특약 메모") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Text("계약·인허가 요건은 지역과 업종에 따라 다릅니다. 관할 지자체, 공인중개사 및 필요시 법률 전문가에게 확인하세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

private val costs09 = listOf("보증금", "권리금", "인테리어·설비", "커피 머신·그라인더", "냉장고·제빙기", "가구·집기", "POS·간판", "초도 원재료·포장재", "인허가·교육·보험", "홍보·가오픈", "운영 예비자금", "기타")
@Composable
fun StartupCost09() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("ourcafe_09", 0) }
    val values = remember { mutableStateMapOf<String, String>().apply { costs09.forEach { put(it, prefs.getString("cost_$it", "") ?: "") } } }
    val total = costs09.sumOf { values[it]?.toLongOrNull() ?: 0L }
    Text("창업 비용 상세 견적표", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("보증금부터 운영 예비자금까지 항목별 예상 금액을 입력해요.")
    Spacer(Modifier.height(10.dp))
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("총 예상 창업 자금")
            Text(money09(total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
    costs09.forEach { label ->
        OutlinedTextField(
            value = values[label] ?: "", onValueChange = { raw ->
                val filtered = raw.filter(Char::isDigit)
                values[label] = filtered
                prefs.edit().putString("cost_$label", filtered).apply()
            }, label = { Text(label) }, suffix = { Text("원") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    Text("보증금은 계약 종료 시 반환될 수 있는 자금이지만 초기 확보액에 포함됩니다. 부가세, 예상 밖 공사비, 운전자금 등을 빠뜨리지 마세요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun DecisionJournal09() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("ourcafe_09", 0) }
    var rows by remember { mutableStateOf(load09(prefs.getString("decisions", null))) }
    var title by remember { mutableStateOf("") }
    var options by remember { mutableStateOf("") }
    var conclusion by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    Text("우리의 결정 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("둘이 결정한 이유를 남겨두면 나중에 다시 비교하기 편해요.")
    OutlinedTextField(title, { title = it }, label = { Text("결정 주제 (예: 동네 카페 vs 테이크아웃)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(options, { options = it }, label = { Text("비교한 선택지") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(conclusion, { conclusion = it }, label = { Text("최종 결정 / 현재 방향") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(reason, { reason = it }, label = { Text("결정 이유 · 다시 확인할 점") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    Button(enabled = title.isNotBlank() && conclusion.isNotBlank(), onClick = {
        rows = rows + JSONObject().put("title", title.trim()).put("options", options.trim()).put("conclusion", conclusion.trim()).put("reason", reason.trim())
        prefs.edit().putString("decisions", persist09(rows)).apply()
        title = ""; options = ""; conclusion = ""; reason = ""
    }, modifier = Modifier.fillMaxWidth()) { Text("결정 저장") }
    rows.asReversed().forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.optString("title"), fontWeight = FontWeight.Bold)
                    if (item.optString("options").isNotBlank()) Text("선택지: ${item.optString("options")}")
                    Text("결정: ${item.optString("conclusion")}", color = MaterialTheme.colorScheme.primary)
                    if (item.optString("reason").isNotBlank()) Text("이유: ${item.optString("reason")}")
                }
                IconButton(onClick = { rows = rows.filterNot { it === item }; prefs.edit().putString("decisions", persist09(rows)).apply() }) { Icon(Icons.Outlined.Delete, "기록 삭제") }
            }
        }
    }
    Text("현재 이 휴대폰에만 저장됩니다. 상대방과 공유하려면 별도 동기화가 필요해요.", style = MaterialTheme.typography.bodySmall)
}
