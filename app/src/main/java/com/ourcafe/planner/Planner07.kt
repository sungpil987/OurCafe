package com.ourcafe.planner

import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun krw07(value: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"
private fun read07(raw: String?): List<JSONObject> = try {
    val array = JSONArray(raw ?: "[]")
    (0 until array.length()).mapNotNull { array.optJSONObject(it) }
} catch (_: Exception) { emptyList() }
private fun write07(rows: List<JSONObject>) = JSONArray(rows).toString()

@Composable
fun PropertyCompare07() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_07", 0) }
    var rows by remember { mutableStateOf(read07(prefs.getString("properties", null))) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sortByRent by remember { mutableStateOf(false) }
    Text("후보 매장의 조건을 나란히 비교하고 기록해요.")
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(name, { name = it }, label = { Text("매물 이름 / 별명") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(address, { address = it }, label = { Text("주소 / 상권") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(deposit, { deposit = it.filter(Char::isDigit) }, label = { Text("보증금 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(rent, { rent = it.filter(Char::isDigit) }, label = { Text("월세 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(area, { area = it }, label = { Text("면적 / 평수") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(notes, { notes = it }, label = { Text("장점·단점·권리금·방문 메모") }, minLines = 2, modifier = Modifier.fillMaxWidth())
    Button(
        enabled = name.isNotBlank() && deposit.toLongOrNull() != null && rent.toLongOrNull() != null,
        onClick = {
            rows = rows + JSONObject().put("name", name.trim()).put("address", address.trim())
                .put("deposit", deposit.toLong()).put("rent", rent.toLong())
                .put("area", area.trim()).put("notes", notes.trim())
            prefs.edit().putString("properties", write07(rows)).apply()
            name = ""; address = ""; deposit = ""; rent = ""; area = ""; notes = ""
        }, modifier = Modifier.fillMaxWidth()
    ) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("후보 매물 저장") }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("후보 ${rows.size}곳", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = { sortByRent = !sortByRent }) { Text(if (sortByRent) "등록순 보기" else "월세 낮은 순") }
    }
    val visible = if (sortByRent) rows.sortedBy { it.optLong("rent") } else rows.asReversed()
    visible.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text(item.optString("name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        rows = rows.filterNot { it === item }
                        prefs.edit().putString("properties", write07(rows)).apply()
                    }) { Icon(Icons.Outlined.Delete, "매물 삭제") }
                }
                if (item.optString("address").isNotBlank()) Text(item.optString("address"))
                Text("보증금 ${krw07(item.optLong("deposit"))}  ·  월세 ${krw07(item.optLong("rent"))}", color = MaterialTheme.colorScheme.primary)
                if (item.optString("area").isNotBlank()) Text("면적: ${item.optString("area")}")
                if (item.optString("notes").isNotBlank()) Text(item.optString("notes"))
            }
        }
    }
    Text("금액은 직접 입력한 값이며, 중개수수료·관리비·부가세 등은 별도 확인이 필요해요.", style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeCalendar07() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_07", 0) }
    var rows by remember { mutableStateOf(read07(prefs.getString("events", null))) }
    var showPicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    fun formatted(ms: Long) = dateFormat.format(Date(ms))
    Text("매물 방문, 장비 상담, 카페 조사 일정을 기록해요.")
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.DateRange, null)
        Spacer(Modifier.width(8.dp))
        Text("날짜 선택: ${formatted(dateMillis)}")
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                dateMillis = state.selectedDateMillis ?: dateMillis
                showPicker = false
            }) { Text("선택") }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } }) {
            DatePicker(state = state)
        }
    }
    OutlinedTextField(title, { title = it }, label = { Text("일정 이름") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(location, { location = it }, label = { Text("장소 / 주소") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(memo, { memo = it }, label = { Text("준비물 / 메모") }, modifier = Modifier.fillMaxWidth())
    Button(enabled = title.isNotBlank(), onClick = {
        rows = rows + JSONObject().put("date", formatted(dateMillis)).put("title", title.trim())
            .put("location", location.trim()).put("memo", memo.trim())
        prefs.edit().putString("events", write07(rows)).apply()
        title = ""; location = ""; memo = ""
    }, modifier = Modifier.fillMaxWidth()) { Text("일정 추가") }
    Spacer(Modifier.height(12.dp))
    Text("등록된 일정 ${rows.size}개", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    rows.sortedBy { it.optString("date") }.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.optString("date"), color = MaterialTheme.colorScheme.primary)
                    Text(item.optString("title"), fontWeight = FontWeight.Bold)
                    if (item.optString("location").isNotBlank()) Text(item.optString("location"))
                    if (item.optString("memo").isNotBlank()) Text(item.optString("memo"), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = {
                    rows = rows.filterNot { it === item }
                    prefs.edit().putString("events", write07(rows)).apply()
                }) { Icon(Icons.Outlined.Delete, "일정 삭제") }
            }
        }
    }
    Text("이 일정은 현재 휴대폰에만 저장돼요. 자동 알림이나 상대방 동기화는 다음 단계에서 지원할 예정이에요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun CafeTools07() {
    val context = LocalContext.current
    Text("OUR CAFÉ 도구함", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("준비 기록 백업 안내", fontWeight = FontWeight.Bold)
            Text("앱을 삭제하면 휴대폰에만 저장된 기록이 사라질 수 있어요. 삭제 전에는 데이터를 별도로 보관해야 해요.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val local = context.getSharedPreferences("ourcafe_local", 0).all
                val v06 = context.getSharedPreferences("ourcafe_06", 0).all
                val v07 = context.getSharedPreferences("ourcafe_07", 0).all
                val export = JSONObject().put("app", "OUR CAFE").put("local", JSONObject(local)).put("v06", JSONObject(v06)).put("v07", JSONObject(v07)).toString(2)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "OUR CAFÉ 개인 기록 백업")
                    putExtra(Intent.EXTRA_TEXT, export)
                }
                context.startActivity(Intent.createChooser(intent, "백업 데이터 공유 / 저장"))
            }) {
                Icon(Icons.Outlined.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("로컬 기록 내보내기")
            }
            Text("사진 파일 자체와 Firebase 공동 공간 데이터는 백업에 포함되지 않아요. 내보낸 파일에는 개인 기록이 포함될 수 있으니 공유 대상에 주의하세요.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
