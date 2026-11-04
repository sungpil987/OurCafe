package com.ourcafe.planner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val green14 = Color(0xFF4F7058)
private val weekdays14 = listOf("일", "월", "화", "수", "목", "금", "토")
private val dateFormat14 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun loadEvents14(raw: String?): List<JSONObject> = try {
    val a = JSONArray(raw ?: "[]")
    (0 until a.length()).mapNotNull { a.optJSONObject(it) }
} catch (_: Exception) { emptyList() }
private fun persist14(prefs: android.content.SharedPreferences, items: List<JSONObject>) {
    prefs.edit().putString("events_14", JSONArray(items).toString()).apply()
}
@Composable
fun MonthlyCalendar14(legacySchedule: List<String>) {
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("ourcafe_calendar_14", 0)
    var events by remember { mutableStateOf(loadEvents14(prefs.getString("events_14", null))) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val start = month.atDay(1)
    val offset = start.dayOfWeek.value % 7
    val totalCells = ((offset + month.lengthOfMonth() + 6) / 7) * 7
    val dayEvents = events.filter { it.optString("date") == selected.format(dateFormat14) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("한눈에 보는 우리의 일정", color = green14, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                    Text("${month.year}년 ${month.monthValue}월", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { month = YearMonth.now(); selected = LocalDate.now() }) { Text("오늘") }
                    TextButton(onClick = { month = month.plusMonths(1) }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
                }
                Row {
                    weekdays14.forEachIndexed { i, name ->
                        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                            color = if (i == 0) Color(0xFFBA645B) else if (i == 6) Color(0xFF5C7BA3) else green14,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                repeat(totalCells / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val index = week * 7 + column - offset + 1
                            val valid = index in 1..month.lengthOfMonth()
                            val day = if (valid) month.atDay(index) else null
                            val selectedDay = day == selected
                            val onDate = if (day != null) events.filter { it.optString("date") == day.format(dateFormat14) } else emptyList()
                            Surface(onClick = { if (day != null) selected = day }, enabled = valid,
                                modifier = Modifier.weight(1f).height(82.dp).padding(2.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedDay) Color(0xFFE0EADD) else Color.Transparent) {
                                Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (valid) index.toString() else "", fontWeight = if (day == today) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (day == today) green14 else if (column == 0) Color(0xFFBA645B) else Color(0xFF303830),
                                        style = MaterialTheme.typography.bodySmall)
                                    onDate.take(2).forEach { item ->
                                        Text("• ${item.optString("title")}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            color = green14, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (onDate.size > 2) Text("+${onDate.size - 2}", color = green14, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${selected.monthValue}월 ${selected.dayOfMonth}일 일정", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { showAdd = true }) { Text("+ 일정 추가") }
        }
        if (dayEvents.isEmpty()) Text("이날 등록된 일정이 없어요.", color = Color.Gray)
        dayEvents.forEach { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.optString("title"), fontWeight = FontWeight.Bold)
                        if (item.optString("note").isNotBlank()) Text(item.optString("note"), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { events = events.filterNot { it === item }; persist14(prefs, events) }) { Text("삭제") }
                }
            }
        }
        if (legacySchedule.isNotEmpty()) {
            HorizontalDivider()
            Text("이전 버전 일정 기록", fontWeight = FontWeight.Bold)
            Text("기존에 작성한 일정은 삭제하지 않고 아래에 보관했어요. 날짜별로 다시 등록하면 달력에 표시됩니다.",
                style = MaterialTheme.typography.bodySmall)
            legacySchedule.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
    if (showAdd) {
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("${selected.monthValue}월 ${selected.dayOfMonth}일 일정 추가") },
            text = {
                Column {
                    OutlinedTextField(title, { title = it }, label = { Text("일정 제목") }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("시간·장소·메모") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    events = events + JSONObject().put("date", selected.format(dateFormat14))
                        .put("title", title.trim()).put("note", note.trim())
                    persist14(prefs, events)
                    title = ""; note = ""; showAdd = false
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("취소") } })
    }
}
