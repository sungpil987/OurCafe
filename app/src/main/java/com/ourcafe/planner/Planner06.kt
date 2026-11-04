package com.ourcafe.planner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat

private fun won(value: Long): String = NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(value) + "원"
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
private fun readRows(raw: String?): List<JSONObject> = try { JSONArray(raw ?: "[]").objects() } catch (_: Exception) { emptyList() }
private fun saveRows(rows: List<JSONObject>): String = JSONArray(rows).toString()

@Composable
fun InteriorBoard06() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_06", 0) }
    var rows by remember { mutableStateOf(readRows(prefs.getString("photos", null))) }
    var caption by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("전체") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            selectedUri = uri
        }
    }
    Text("인테리어 레퍼런스를 사진과 메모로 모아보세요.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.AddPhotoAlternate, null)
        Spacer(Modifier.width(8.dp))
        Text("갤러리에서 사진 선택")
    }
    selectedUri?.let { uri ->
        PhotoPreview06(uri.toString())
        OutlinedTextField(caption, { caption = it }, label = { Text("사진 설명 / 인테리어 아이디어") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            rows = rows + JSONObject().put("uri", uri.toString()).put("memo", caption.trim())
            prefs.edit().putString("photos", saveRows(rows)).apply()
            selectedUri = null
            caption = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("보드에 저장") }
    }
    Spacer(Modifier.height(12.dp))
    Text("저장된 사진 · ${rows.size}장", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("사진은 현재 이 휴대폰에만 저장됩니다. 원본 사진을 삭제하거나 이동하면 표시되지 않을 수 있어요.", style = MaterialTheme.typography.bodySmall)
    rows.asReversed().forEach { item ->
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(Modifier.padding(12.dp)) {
                PhotoPreview06(item.optString("uri"))
                if (item.optString("memo").isNotBlank()) Text(item.optString("memo"), modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = {
                    rows = rows.filterNot { it === item }
                    prefs.edit().putString("photos", saveRows(rows)).apply()
                }) { Text("보드에서 삭제") }
            }
        }
    }
}

@Composable
private fun PhotoPreview06(uri: String) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val parsed = Uri.parse(uri)
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, parsed)) { decoder, _, _ ->
                    decoder.setTargetSampleSize(3)
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, parsed)
            }
        } catch (_: Exception) { null }
    }
    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "인테리어 참고 사진", modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
    else Text("사진을 표시할 수 없습니다.")
}

@Composable
fun BudgetCalculator06(existingExpenses: List<String>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_06", 0) }
    var target by remember { mutableStateOf(prefs.getString("budget_target", "") ?: "") }
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(readRows(prefs.getString("budget_rows", null))) }
    val planned = rows.sumOf { it.optLong("amount") }
    val spent = existingExpenses.sumOf { it.substringBefore("|").toLongOrNull() ?: 0L }
    val goal = target.toLongOrNull() ?: 0L
    val remaining = goal - planned - spent
    Text("창업 목표 금액과 예상 비용을 관리해요.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(target, { target = it.filter(Char::isDigit); prefs.edit().putString("budget_target", target).apply() }, label = { Text("창업 목표 예산 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("목표 예산  ${won(goal)}", fontWeight = FontWeight.Bold)
            Text("예상 비용  ${won(planned)}")
            Text("기존 지출 기록  ${won(spent)}")
            HorizontalDivider()
            Text("남은 예산  ${won(remaining)}", style = MaterialTheme.typography.titleLarge, color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (goal > 0) LinearProgressIndicator(progress = { ((planned + spent).toFloat() / goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("예상 비용과 기존 지출은 별도로 합산됩니다. 같은 비용을 두 번 입력하지 않도록 확인해 주세요.", style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(title, { title = it }, label = { Text("예상 비용 항목 (보증금, 장비, 인테리어 등)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("예상 금액 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Button(enabled = title.isNotBlank() && price.toLongOrNull() != null, onClick = {
        rows = rows + JSONObject().put("name", title.trim()).put("amount", price.toLong())
        prefs.edit().putString("budget_rows", saveRows(rows)).apply()
        title = ""; price = ""
    }, modifier = Modifier.fillMaxWidth()) { Text("예상 비용 추가") }
    rows.forEach { item ->
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.optString("name"), fontWeight = FontWeight.SemiBold)
                    Text(won(item.optLong("amount")))
                }
                IconButton(onClick = { rows = rows.filterNot { it === item }; prefs.edit().putString("budget_rows", saveRows(rows)).apply() }) {
                    Icon(Icons.Outlined.Delete, "삭제")
                }
            }
        }
    }
}

@Composable
fun MenuLab06() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ourcafe_06", 0) }
    var rows by remember { mutableStateOf(readRows(prefs.getString("menu_rows", null))) }
    var name by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var recipe by remember { mutableStateOf("") }
    Text("한 잔당 재료비와 판매가로 예상 마진을 계산해요.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(name, { name = it }, label = { Text("메뉴 이름") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(ingredients, { ingredients = it }, label = { Text("재료 목록") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(cost, { cost = it.filter(Char::isDigit) }, label = { Text("1잔 재료비 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("판매가 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(recipe, { recipe = it }, label = { Text("레시피 / 시식 메모") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    val c = cost.toLongOrNull() ?: 0L
    val p = price.toLongOrNull() ?: 0L
    if (p > 0) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("예상 재료 원가율  ${"%.1f".format(java.util.Locale.KOREA, c.toDouble() * 100 / p)}%")
                Text("재료비 차감 마진  ${won(p - c)}")
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(enabled = name.isNotBlank() && price.toLongOrNull() != null && p > 0 && cost.toLongOrNull() != null, onClick = {
        rows = rows + JSONObject().put("name", name.trim()).put("ingredients", ingredients.trim()).put("cost", c).put("price", p).put("recipe", recipe.trim())
        prefs.edit().putString("menu_rows", saveRows(rows)).apply()
        name = ""; ingredients = ""; cost = ""; price = ""; recipe = ""
    }, modifier = Modifier.fillMaxWidth()) { Text("메뉴 저장") }
    Text("마진은 재료비만 차감한 금액으로, 인건비·임대료·수수료·세금은 포함하지 않아요.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(10.dp))
    rows.asReversed().forEach { item ->
        val sale = item.optLong("price")
        val material = item.optLong("cost")
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalCafe, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(item.optString("name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { rows = rows.filterNot { it === item }; prefs.edit().putString("menu_rows", saveRows(rows)).apply() }) { Icon(Icons.Outlined.Delete, "삭제") }
                }
                Text("판매가 ${won(sale)} · 재료비 ${won(material)}")
                Text("원가율 ${if (sale > 0) "%.1f".format(java.util.Locale.KOREA, material.toDouble() * 100 / sale) else "0"}% · 마진 ${won(sale - material)}", color = MaterialTheme.colorScheme.primary)
                if (item.optString("ingredients").isNotBlank()) Text("재료: ${item.optString("ingredients")}")
                if (item.optString("recipe").isNotBlank()) Text("레시피: ${item.optString("recipe")}")
            }
        }
    }
}
