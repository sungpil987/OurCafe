package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val sections16 = listOf("무드보드", "배치도", "디자인 투표", "구매 리스트", "견적 비교", "결정 기록")
private fun read16(raw: String?): List<String> = runCatching {
    val a = JSONArray(raw ?: "[]")
    (0 until a.length()).map { a.getString(it) }
}.getOrDefault(emptyList())

@Composable
fun DesignStudio16() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_design16", 0)
    var section by remember { mutableIntStateOf(0) }
    var records by remember(section) { mutableStateOf(read16(prefs.getString("section_$section", "[]"))) }
    var title by remember(section) { mutableStateOf("") }
    var details by remember(section) { mutableStateOf("") }
    var amount by remember(section) { mutableStateOf("") }
    var link by remember(section) { mutableStateOf("") }
    var editing by remember(section) { mutableIntStateOf(-1) }
    var width by remember { mutableStateOf(prefs.getString("room_width", "") ?: "") }
    var depth by remember { mutableStateOf(prefs.getString("room_depth", "") ?: "") }
    var tableCount by remember { mutableStateOf(prefs.getString("room_tables", "") ?: "") }
    fun save(next: List<String>) {
        records = next
        prefs.edit().putString("section_$section", JSONArray(next).toString()).apply()
    }
    fun clear() { title = ""; details = ""; amount = ""; link = ""; editing = -1 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("DESIGN STUDIO · 우리 카페 디자인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("기존 인테리어 보드·브랜드 노트·견적표는 변경하지 않고 새 디자인 기록을 별도 보관합니다.", style = MaterialTheme.typography.bodySmall)
        sections16.chunked(3).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                group.forEach { label ->
                    val idx = sections16.indexOf(label)
                    FilterChip(selected = section == idx, onClick = { section = idx }, label = { Text(label, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                }
            }
        }
        if (section == 1) {
            Text("매장 배치 계획", style = MaterialTheme.typography.titleMedium)
            Text("실측한 매장 크기와 구역별 배치 아이디어를 기록하세요. 도면 자동 생성 기능은 아닙니다.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(width, { width = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("가로 길이 (m)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(depth, { depth = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("세로 길이 (m)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tableCount, { tableCount = it.filter(Char::isDigit) }, label = { Text("예상 테이블 수") }, modifier = Modifier.fillMaxWidth())
            val area = (width.toDoubleOrNull() ?: 0.0) * (depth.toDoubleOrNull() ?: 0.0)
            Text("단순 직사각형 면적: %.1f㎡".format(area))
            Button(onClick = {
                prefs.edit().putString("room_width", width).putString("room_depth", depth).putString("room_tables", tableCount).apply()
            }) { Text("매장 크기 저장") }
        }
        OutlinedTextField(title, { title = it }, label = { Text(when (section) {
            0 -> "콘셉트·레퍼런스 이름"; 1 -> "구역 이름 (카운터·주방·테이블 등)"
            2 -> "투표할 디자인 후보"; 3 -> "구매할 물품"; 4 -> "견적 업체·항목"; else -> "결정한 디자인"
        }) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(details, { details = it }, label = { Text("설명·선택 이유·비교 메모") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        if (section == 0 || section == 3 || section == 4) {
            OutlinedTextField(link, { link = it }, label = { Text("참고 이미지·구매·견적 링크") }, modifier = Modifier.fillMaxWidth())
        }
        if (section == 3 || section == 4) {
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("예상 금액 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
        Button(enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            val obj = JSONObject().put("title", title.trim()).put("details", details.trim())
                .put("amount", amount.toLongOrNull() ?: 0L).put("link", link.trim())
                .put("yes", 0).put("no", 0).put("done", false)
            val next = records.toMutableList()
            if (editing in next.indices) {
                val old = runCatching { JSONObject(next[editing]) }.getOrNull()
                if (old != null) {
                    obj.put("yes", old.optInt("yes")).put("no", old.optInt("no")).put("done", old.optBoolean("done"))
                }
                next[editing] = obj.toString()
            } else next.add(obj.toString())
            save(next); clear()
        }) { Text(if (editing >= 0) "수정 저장" else "새 기록 저장") }
        if (editing >= 0) TextButton(onClick = { clear() }) { Text("수정 취소") }
        if (section == 3 || section == 4) {
            val total = records.sumOf { runCatching { JSONObject(it).optLong("amount") }.getOrDefault(0L) }
            Text("등록 금액 합계: ${total}원", fontWeight = FontWeight.Bold)
        }
        Text("기록 ${records.size}개", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        records.forEachIndexed { index, raw ->
            val item = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEachIndexed
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.optString("title"), fontWeight = FontWeight.Bold)
                    if (item.optString("details").isNotBlank()) Text(item.optString("details"))
                    if (item.optString("link").isNotBlank()) Text(item.optString("link"), style = MaterialTheme.typography.bodySmall)
                    if (section == 3 || section == 4) Text("금액: ${item.optLong("amount")}원")
                    if (section == 2) {
                        Text("찬성 ${item.optInt("yes")} · 반대 ${item.optInt("no")}")
                        Row {
                            TextButton(onClick = {
                                val next = records.toMutableList()
                                next[index] = JSONObject(raw).put("yes", item.optInt("yes") + 1).toString()
                                save(next)
                            }) { Text("찬성 +1") }
                            TextButton(onClick = {
                                val next = records.toMutableList()
                                next[index] = JSONObject(raw).put("no", item.optInt("no") + 1).toString()
                                save(next)
                            }) { Text("반대 +1") }
                        }
                    }
                    if (section == 3) {
                        Row {
                            Checkbox(checked = item.optBoolean("done"), onCheckedChange = { done ->
                                val next = records.toMutableList()
                                next[index] = JSONObject(raw).put("done", done).toString()
                                save(next)
                            })
                            Text("구매 완료", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    Row {
                        TextButton(onClick = {
                            editing = index; title = item.optString("title")
                            details = item.optString("details"); amount = item.optLong("amount").toString()
                            link = item.optString("link")
                        }) { Text("수정") }
                        TextButton(onClick = {
                            save(records.filterIndexed { i, _ -> i != index })
                            if (editing == index) clear()
                        }) { Text("삭제") }
                    }
                }
            }
        }
        Text("투표는 이 기기에서 누적하는 방식입니다. 실시간 공동 투표나 실제 도면 편집은 지원하지 않습니다.", style = MaterialTheme.typography.bodySmall)
    }
}
