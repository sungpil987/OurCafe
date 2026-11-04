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

private fun read12(raw: String?): List<JSONObject> = try {
    val a = JSONArray(raw ?: "[]")
    (0 until a.length()).mapNotNull { a.optJSONObject(it) }
} catch (_: Exception) { emptyList() }
private fun won12(v: Double) = NumberFormat.getNumberInstance(Locale.KOREA).format(v.toLong()) + "원"
private fun n12(v: String) = v.toDoubleOrNull() ?: 0.0
private fun save12(p: android.content.SharedPreferences, key: String, rows: List<JSONObject>) {
    p.edit().putString(key, JSONArray(rows).toString()).apply()
}
@Composable private fun Field12(label: String, value: String, number: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(if (number) it.filter { c -> c.isDigit() || c == '.' } else it) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
}

@Composable
fun IngredientPrices12() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_12", 0)
    var rows by remember { mutableStateOf(read12(prefs.getString("ingredients", null))) }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var price by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    Text("원재료 구매 단가표", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("구매 가격과 총 용량을 등록하면 레시피 계산에 사용할 단위당 가격이 만들어져요.")
    Field12("원재료 이름 (예: 원두)", name) { name = it }
    Field12("구매 용량 (예: 1000)", quantity, true) { quantity = it }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("g", "ml", "개").forEach { choice ->
            FilterChip(selected = unit == choice, onClick = { unit = choice }, label = { Text(choice) })
        }
    }
    Field12("구매 총액 (원)", price, true) { price = it }
    Field12("구매처·브랜드 메모", vendor) { vendor = it }
    if (n12(quantity) > 0) Text("1$unit 당 ${won12(n12(price) / n12(quantity))}")
    Button(enabled = name.isNotBlank() && n12(quantity) > 0 && n12(price) > 0, modifier = Modifier.fillMaxWidth(), onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("quantity", n12(quantity)).put("unit", unit)
            .put("price", n12(price)).put("vendor", vendor.trim())
        save12(prefs, "ingredients", rows)
        name = ""; quantity = ""; price = ""; vendor = ""
    }) { Text("원재료 단가 저장") }
    rows.forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.optString("name"), fontWeight = FontWeight.Bold)
                    val q = item.optDouble("quantity", 0.0)
                    Text("${item.optDouble("quantity")} ${item.optString("unit")} · ${won12(item.optDouble("price"))}")
                    if (q > 0) Text("1${item.optString("unit")}당 ${won12(item.optDouble("price") / q)} · ${item.optString("vendor")}")
                }
                IconButton(onClick = { rows = rows.filterNot { it === item }; save12(prefs, "ingredients", rows) }) {
                    Icon(Icons.Outlined.Delete, "삭제")
                }
            }
        }
    }
    Text("동일 원재료라도 구매처·규격에 따라 가격이 달라집니다. 레시피에서는 선택한 구매 규격을 기준으로 계산해요.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun RecipeCost12() {
    val prefs = LocalContext.current.getSharedPreferences("ourcafe_12", 0)
    val ingredients = read12(prefs.getString("ingredients", null))
    var recipes by remember { mutableStateOf(read12(prefs.getString("recipe_costs", null))) }
    var name by remember { mutableStateOf("") }
    var sale by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var waste by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }
    var used by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf<JSONObject>()) }
    val material = lines.sumOf { it.optDouble("cost") }
    val variableCost = material + n12(packaging) + n12(fee) + n12(waste)
    val contribution = n12(sale) - variableCost
    Text("레시피 자동 원가 계산", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("단가표에서 원재료를 선택하고 사용량을 입력하면 재료비가 자동 합산돼요.")
    if (ingredients.isEmpty()) {
        Text("먼저 ‘원재료 단가표’에서 원두·우유 등 구매 가격을 등록하세요.", color = MaterialTheme.colorScheme.error)
        return
    }
    Field12("메뉴 이름", name) { name = it }
    Field12("판매 가격 (원)", sale, true) { sale = it }
    Text("원재료 선택", fontWeight = FontWeight.Bold)
    ingredients.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == index, onClick = { selected = index })
            Text("${item.optString("name")} (${item.optString("unit")})")
        }
    }
    Field12("사용량 (${ingredients[selected.coerceIn(0, ingredients.lastIndex)].optString("unit")})", used, true) { used = it }
    OutlinedButton(enabled = n12(used) > 0, onClick = {
        val item = ingredients[selected.coerceIn(0, ingredients.lastIndex)]
        val q = item.optDouble("quantity", 0.0)
        if (q > 0) {
            lines = lines + JSONObject().put("name", item.optString("name"))
                .put("used", n12(used)).put("unit", item.optString("unit"))
                .put("cost", n12(used) * item.optDouble("price") / q)
            used = ""
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("레시피에 원재료 추가") }
    lines.forEachIndexed { index, line ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${line.optString("name")} ${line.optDouble("used")} ${line.optString("unit")} · ${won12(line.optDouble("cost"))}", modifier = Modifier.weight(1f))
            IconButton(onClick = { lines = lines.filterIndexed { i, _ -> i != index } }) { Icon(Icons.Outlined.Delete, "제거") }
        }
    }
    Field12("포장재·컵 비용 (원)", packaging, true) { packaging = it }
    Field12("결제·플랫폼 수수료 (원)", fee, true) { fee = it }
    Field12("예상 폐기·손실 배분액 (원)", waste, true) { waste = it }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("재료 원가: ${won12(material)}")
            Text("총 변동비: ${won12(variableCost)}")
            Text("주문당 기여이익: ${won12(contribution)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("기여이익률: ${if (n12(sale) > 0) (contribution / n12(sale) * 100).toInt() else 0}%")
        }
    }
    Button(enabled = name.isNotBlank() && n12(sale) > 0 && lines.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
        recipes = recipes + JSONObject().put("name", name.trim()).put("sale", n12(sale))
            .put("materials", material).put("cost", variableCost).put("margin", contribution)
            .put("lines", JSONArray(lines))
        save12(prefs, "recipe_costs", recipes)
        name = ""; sale = ""; packaging = ""; fee = ""; waste = ""; lines = emptyList()
    }) { Text("메뉴 원가 결과 저장") }
    Text("저장한 계산 결과", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    recipes.asReversed().forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.optString("name"), fontWeight = FontWeight.Bold)
                    Text("판매가 ${won12(item.optDouble("sale"))} / 변동비 ${won12(item.optDouble("cost"))}")
                    Text("기여이익 ${won12(item.optDouble("margin"))}")
                }
                IconButton(onClick = { recipes = recipes.filterNot { it === item }; save12(prefs, "recipe_costs", recipes) }) {
                    Icon(Icons.Outlined.Delete, "삭제")
                }
            }
        }
    }
    Text("저장된 계산은 당시 단가 기준의 기록입니다. 원재료 가격을 변경하면 새로 계산해야 해요. 인건비·임대료·세금은 포함되지 않습니다.", style = MaterialTheme.typography.bodySmall)
}
