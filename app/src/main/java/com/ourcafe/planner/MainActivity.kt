package com.ourcafe.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray

private val sage = Color(0xFF55745F)
private val cream = Color(0xFFF7F5EE)
private val roadmap = listOf(
    "창업 예산 정하기", "카페 콘셉트 정하기", "고객층 정의하기", "경쟁 카페 조사",
    "상권 분석하기", "손익분기점 계산", "매물 후보 비교", "임대차 계약 검토",
    "브랜드 이름과 로고", "인테리어 견적 비교", "커피 장비 선정",
    "메뉴와 원가표", "인허가 준비", "가오픈 테스트", "정식 오픈"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("ourcafe_local", MODE_PRIVATE)
        fun readList(key: String): List<String> = try {
            val arr = JSONArray(prefs.getString(key, "[]"))
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
        setContent {
            var tab by remember { mutableIntStateOf(0) }
            var notes by remember { mutableStateOf(readList("notes")) }
            var tasks by remember { mutableStateOf(readList("tasks")) }
            var completed by remember { mutableStateOf(readList("completed").toSet()) }
            var expenses by remember { mutableStateOf(readList("expenses")) }
            var draft by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }
            fun save(key: String, values: Collection<String>) {
                prefs.edit().putString(key, JSONArray(values.toList()).toString()).apply()
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = sage, background = cream, surface = Color.White)) {
                Scaffold(bottomBar = {
                    NavigationBar {
                        listOf("홈", "로드맵", "메모", "할 일", "예산").forEachIndexed { i, title ->
                            NavigationBarItem(selected = tab == i, onClick = { tab = i; draft = ""; amount = "" },
                                icon = { Text(listOf("⌂", "✓", "✎", "☑", "₩")[i]) }, label = { Text(title) })
                        }
                    }
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                        Spacer(Modifier.height(20.dp))
                        Text("OUR CAFÉ ☕", color = sage, style = MaterialTheme.typography.labelLarge)
                        Text(listOf("우리의 카페 만들기", "창업 로드맵", "아이디어 메모", "함께 할 일", "창업 예산")[tab],
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        if (tab == 0) {
                            val done = completed.size
                            InfoCard("창업 준비 진행률", "$done / ${roadmap.size} 단계 완료")
                            LinearProgressIndicator(progress = { done.toFloat() / roadmap.size }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            InfoCard("메모", "${notes.size}개 기록")
                            InfoCard("할 일", "${tasks.size}개 등록")
                            InfoCard("지출", "${expenses.sumOf { it.substringBefore("|").toLongOrNull() ?: 0L }}원")
                            Text("현재 기록은 이 기기에만 저장됩니다. 두 사람 동기화는 다음 버전에서 제공됩니다.",
                                color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        } else {
                            if (tab == 2 || tab == 3 || tab == 4) {
                                OutlinedTextField(value = draft, onValueChange = { draft = it },
                                    label = { Text(when(tab) { 2 -> "새 메모"; 3 -> "새 할 일"; else -> "지출 항목" }) },
                                    modifier = Modifier.fillMaxWidth())
                                if (tab == 4) OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) },
                                    label = { Text("금액 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    if (draft.isNotBlank() && (tab != 4 || amount.isNotBlank())) {
                                        when (tab) {
                                            2 -> { notes = notes + draft.trim(); save("notes", notes) }
                                            3 -> { tasks = tasks + draft.trim(); save("tasks", tasks) }
                                            4 -> { expenses = expenses + "$amount|${draft.trim()}"; save("expenses", expenses) }
                                        }
                                        draft = ""; amount = ""
                                    }
                                }, enabled = draft.isNotBlank() && (tab != 4 || amount.isNotBlank())) { Text("추가하기") }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                                when(tab) {
                                    1 -> items(roadmap) { item ->
                                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                            Row(Modifier.padding(12.dp)) {
                                                Checkbox(checked = item in completed, onCheckedChange = { checked ->
                                                    completed = if (checked) completed + item else completed - item
                                                    save("completed", completed)
                                                })
                                                Text(item, Modifier.padding(top = 13.dp))
                                            }
                                        }
                                    }
                                    2 -> items(notes.indices.toList()) { index ->
                                        EntryCard(notes[index]) { notes = notes.filterIndexed { i, _ -> i != index }; save("notes", notes) }
                                    }
                                    3 -> items(tasks.indices.toList()) { index ->
                                        EntryCard(tasks[index]) { tasks = tasks.filterIndexed { i, _ -> i != index }; save("tasks", tasks) }
                                    }
                                    4 -> {
                                        item { InfoCard("총 지출", "${expenses.sumOf { it.substringBefore("|").toLongOrNull() ?: 0L }}원") }
                                        items(expenses.indices.toList()) { index ->
                                            val e = expenses[index]
                                            EntryCard("${e.substringAfter("|")} · ${e.substringBefore("|")}원") {
                                                expenses = expenses.filterIndexed { i, _ -> i != index }; save("expenses", expenses)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun InfoCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = sage, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(5.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable private fun EntryCard(value: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp)) {
            Text(value, Modifier.weight(1f).padding(top = 10.dp))
            TextButton(onClick = onDelete) { Text("삭제") }
        }
    }
}
