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
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private fun won14(value: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

@Composable
fun RecipeStudio14() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_14", 0)
    fun read(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("recipe_studio", "[]"))
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
    var rows by remember { mutableStateOf(read()) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("커피") }
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("3") }
    var feedback by remember { mutableStateOf("") }
    var photoLink by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val categories = listOf("커피", "논커피", "티", "디저트", "기타")
    fun persist(values: List<String>) {
        rows = values
        prefs.edit().putString("recipe_studio", JSONArray(values).toString()).apply()
    }
    fun clear() {
        editIndex = -1
        name = ""; category = "커피"; ingredients = ""; steps = ""
        cost = ""; price = ""; rating = "3"; feedback = ""; photoLink = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("RECIPE · 메뉴 연구소 2.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("기존 메뉴·표준 레시피는 그대로 유지됩니다. 이 화면은 별도 메뉴 실험 기록장입니다.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("메뉴 이름") }, modifier = Modifier.fillMaxWidth())
        Text("메뉴 분류", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            categories.take(3).forEach { item ->
                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.drop(3).forEach { item ->
                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
            }
        }
        OutlinedTextField(ingredients, { ingredients = it }, label = { Text("재료 및 계량 (g/ml/샷)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(steps, { steps = it }, label = { Text("제조 순서") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(photoLink, { photoLink = it }, label = { Text("메뉴 사진 링크 (선택)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(cost, { cost = it.filter(Char::isDigit) }, label = { Text("1개 원가") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("판매가") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        val costValue = cost.toLongOrNull() ?: 0L
        val priceValue = price.toLongOrNull() ?: 0L
        val profit = priceValue - costValue
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("메뉴 예상 이익: ${won14(profit)}", fontWeight = FontWeight.Bold)
                Text("원가율: " + if (priceValue > 0) "%.1f%%".format(costValue.toDouble() / priceValue * 100) else "판매가를 입력하세요")
                Text("재료 외 포장비·수수료·폐기·세금은 별도 계산해야 합니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("시음·시식 평가", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n ->
                FilterChip(selected = rating == n.toString(), onClick = { rating = n.toString() }, label = { Text("$n") })
            }
        }
        OutlinedTextField(feedback, { feedback = it }, label = { Text("시식 의견·개선할 점") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = name.isNotBlank() && priceValue >= 0L, modifier = Modifier.fillMaxWidth(), onClick = {
            val obj = JSONObject().put("name", name.trim()).put("category", category)
                .put("ingredients", ingredients.trim()).put("steps", steps.trim())
                .put("cost", costValue).put("price", priceValue)
                .put("rating", rating.toIntOrNull() ?: 3).put("feedback", feedback.trim())
                .put("photoLink", photoLink.trim())
            val updated = rows.toMutableList()
            if (editIndex in updated.indices) updated[editIndex] = obj.toString() else updated.add(obj.toString())
            persist(updated)
            clear()
        }) { Text(if (editIndex >= 0) "메뉴 수정 저장" else "메뉴 실험 저장") }
        if (editIndex >= 0) TextButton(onClick = { clear() }) { Text("수정 취소") }
        HorizontalDivider()
        Text("저장된 메뉴 실험 ${rows.size}개", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(search, { search = it }, label = { Text("메뉴 검색") }, modifier = Modifier.fillMaxWidth())
        rows.forEachIndexed { index, raw ->
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEachIndexed
            if (search.isNotBlank() && !obj.optString("name").contains(search.trim(), ignoreCase = true)) return@forEachIndexed
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(obj.optString("name") + " · " + obj.optString("category"), fontWeight = FontWeight.Bold)
                    Text("판매가 ${won14(obj.optLong("price"))} / 원가 ${won14(obj.optLong("cost"))} / 예상 이익 ${won14(obj.optLong("price") - obj.optLong("cost"))}")
                    Text("시식 점수 ${obj.optInt("rating", 3)}/5")
                    if (obj.optString("ingredients").isNotBlank()) Text("재료: " + obj.optString("ingredients"))
                    if (obj.optString("steps").isNotBlank()) Text("제조: " + obj.optString("steps"))
                    if (obj.optString("feedback").isNotBlank()) Text("의견: " + obj.optString("feedback"))
                    if (obj.optString("photoLink").isNotBlank()) Text("사진 링크: " + obj.optString("photoLink"), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = {
                            editIndex = index
                            name = obj.optString("name"); category = obj.optString("category", "커피")
                            ingredients = obj.optString("ingredients"); steps = obj.optString("steps")
                            cost = obj.optLong("cost").toString(); price = obj.optLong("price").toString()
                            rating = obj.optInt("rating", 3).toString(); feedback = obj.optString("feedback")
                            photoLink = obj.optString("photoLink")
                        }) { Text("수정") }
                        TextButton(onClick = {
                            persist(rows.filterIndexed { i, _ -> i != index })
                            if (editIndex == index) clear()
                        }) { Text("삭제") }
                    }
                }
            }
        }
        Text("이 메뉴 실험 기록은 이 휴대폰에 저장되며, 공동 공간과 자동 동기화되지 않습니다.", style = MaterialTheme.typography.bodySmall)
    }
}
