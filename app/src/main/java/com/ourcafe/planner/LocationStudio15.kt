package com.ourcafe.planner

import android.content.Intent
import android.net.Uri
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
import org.json.JSONObject

@Composable
fun LocationStudio15() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_location15", 0) }
    var rows by remember {
        mutableStateOf(runCatching {
            val a = JSONArray(prefs.getString("locations", "[]"))
            (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList()))
    }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var premium by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var parking by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }
    var editing by remember { mutableIntStateOf(-1) }
    var search by remember { mutableStateOf("") }
    val checklist = listOf("전기 용량", "급배수", "환기", "화장실", "소음", "공사 가능", "인허가", "누수", "간판", "계약 특약")
    var checked by remember { mutableStateOf(emptySet<String>()) }
    fun save(items: List<String>) {
        rows = items
        prefs.edit().putString("locations", JSONArray(items).toString()).apply()
    }
    fun reset() {
        editing = -1; name = ""; address = ""; deposit = ""; rent = ""; fee = ""
        premium = ""; area = ""; parking = false; memo = ""; checked = emptySet()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("LOCATION · 상권·매물 연구소", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("기존 매물과 상권 기록은 유지됩니다. 이곳의 새 후보지는 별도로 저장돼요.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("매물 이름") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("주소") }, modifier = Modifier.fillMaxWidth())
        listOf("보증금" to deposit, "월세" to rent, "관리비" to fee, "권리금" to premium, "면적(㎡)" to area).forEach { (label, value) ->
            OutlinedTextField(value, { next ->
                val digits = next.filter(Char::isDigit)
                when (label) {
                    "보증금" -> deposit = digits
                    "월세" -> rent = digits
                    "관리비" -> fee = digits
                    "권리금" -> premium = digits
                    else -> area = digits
                }
            }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
        Row {
            Checkbox(checked = parking, onCheckedChange = { parking = it })
            Text("주차 가능", modifier = Modifier.padding(top = 12.dp))
        }
        Text("현장 방문 체크리스트", fontWeight = FontWeight.Bold)
        checklist.forEach { item ->
            Row {
                Checkbox(checked = item in checked, onCheckedChange = { yes ->
                    checked = if (yes) checked + item else checked - item
                })
                Text(item, modifier = Modifier.padding(top = 12.dp))
            }
        }
        OutlinedTextField(memo, { memo = it }, label = { Text("상권 평가·방문 후기·주의사항") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        val monthly = (rent.toLongOrNull() ?: 0L) + (fee.toLongOrNull() ?: 0L)
        Text("월 임차비: ${monthly}원", fontWeight = FontWeight.Bold)
        Button(enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            val obj = JSONObject().put("name", name.trim()).put("address", address.trim())
                .put("deposit", deposit.toLongOrNull() ?: 0L).put("rent", rent.toLongOrNull() ?: 0L)
                .put("fee", fee.toLongOrNull() ?: 0L).put("premium", premium.toLongOrNull() ?: 0L)
                .put("area", area.toLongOrNull() ?: 0L).put("parking", parking)
                .put("memo", memo.trim()).put("checks", JSONArray(checked.toList()))
            val next = rows.toMutableList()
            if (editing in next.indices) next[editing] = obj.toString() else next.add(obj.toString())
            save(next); reset()
        }) { Text(if (editing >= 0) "수정 저장" else "매물 저장") }
        if (editing >= 0) TextButton(onClick = { reset() }) { Text("수정 취소") }
        HorizontalDivider()
        Text("저장한 매물 ${rows.size}개", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(search, { search = it }, label = { Text("매물 검색") }, modifier = Modifier.fillMaxWidth())
        rows.forEachIndexed { index, raw ->
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEachIndexed
            if (search.isNotBlank() && !(obj.optString("name") + obj.optString("address")).contains(search, true)) return@forEachIndexed
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(obj.optString("name"), fontWeight = FontWeight.Bold)
                    Text(obj.optString("address"))
                    Text("보증금 ${obj.optLong("deposit")}원 · 권리금 ${obj.optLong("premium")}원")
                    Text("월세 ${obj.optLong("rent")}원 · 관리비 ${obj.optLong("fee")}원")
                    Text("면적 ${obj.optLong("area")}㎡ · 주차 ${if (obj.optBoolean("parking")) "가능" else "미확인/불가"}")
                    if (obj.optString("memo").isNotBlank()) Text(obj.optString("memo"))
                    Row {
                        TextButton(enabled = obj.optString("address").isNotBlank(), onClick = {
                            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(obj.optString("address")))
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) { Text("지도") }
                        TextButton(onClick = {
                            editing = index
                            name = obj.optString("name"); address = obj.optString("address")
                            deposit = obj.optLong("deposit").toString(); rent = obj.optLong("rent").toString()
                            fee = obj.optLong("fee").toString(); premium = obj.optLong("premium").toString()
                            area = obj.optLong("area").toString(); parking = obj.optBoolean("parking")
                            memo = obj.optString("memo")
                            val a = obj.optJSONArray("checks") ?: JSONArray()
                            checked = (0 until a.length()).map { a.optString(it) }.toSet()
                        }) { Text("수정") }
                        TextButton(onClick = {
                            save(rows.filterIndexed { i, _ -> i != index })
                            if (editing == index) reset()
                        }) { Text("삭제") }
                    }
                }
            }
        }
        Text("이 기능은 휴대폰에만 저장되며 공동 공간과 자동 동기화되지 않습니다.", style = MaterialTheme.typography.bodySmall)
    }
}
