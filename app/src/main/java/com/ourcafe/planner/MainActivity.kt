package com.ourcafe.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray

private val forest = Color(0xFF4F7058)
private val cream = Color(0xFFF5F2EA)
private val muted = Color(0xFF7D897C)
private val roadmap = listOf(
    "창업 예산 정하기", "카페 콘셉트 정하기", "고객층 정의하기", "경쟁 카페 조사",
    "상권 분석하기", "손익분기점 계산", "매물 후보 비교", "임대차 계약 검토",
    "브랜드 이름과 로고", "인테리어 견적 비교", "커피 장비 선정",
    "메뉴와 원가표", "인허가 준비", "가오픈 테스트", "정식 오픈"
)
private data class Tile(val icon: ImageVector, val title: String, val subtitle: String, val destination: String)
private val tiles = listOf(
    Tile(Icons.Outlined.Assignment, "창업 로드맵", "실전 팁·체크리스트·계약 주의", "roadmap"),
    Tile(Icons.Outlined.AccountBalanceWallet, "예산 관리", "지출·월세·견적", "expenses"),
    Tile(Icons.Outlined.Description, "우리의 메모", "아이디어·회의록", "notes"),
    Tile(Icons.Outlined.Image, "인테리어 보드", "사진·디자인·레퍼런스", "interior"),
    Tile(Icons.Outlined.Place, "상권·매물", "후보지 비교·임대정보", "properties"),
    Tile(Icons.Outlined.LocalCafe, "메뉴 연구소", "레시피·원가·시식", "menus"),
    Tile(Icons.Outlined.Calculate, "창업 견적표", "12개 비용 항목 총합", "startupcost"),
    Tile(Icons.Outlined.FactCheck, "계약 점검", "계약 전 12가지 확인", "contract"),
    Tile(Icons.Outlined.StarRate, "매물 점수", "100점 상권 평가", "scorer"),
    Tile(Icons.Outlined.EditNote, "우리의 결정", "선택지·결정 이유 기록", "decisions"),
    Tile(Icons.Outlined.Storefront, "경쟁 카페 조사", "방문·가격·차별점 기록", "competitors"),
    Tile(Icons.Outlined.RequestQuote, "장비·공사 견적", "업체별 가격·AS 비교", "quotes"),
    Tile(Icons.Outlined.PriceCheck, "메뉴별 이익", "포장·수수료·폐기 반영", "menuprofit"),
    Tile(Icons.Outlined.EventAvailable, "오픈 준비", "15개 실무 체크리스트", "launch")
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
            var page by remember { mutableStateOf<String?>(null) }
            var notes by remember { mutableStateOf(readList("notes")) }
            var tasks by remember { mutableStateOf(readList("tasks")) }
            var completed by remember { mutableStateOf(readList("completed").toSet()) }
            var guideCompleted by remember { mutableStateOf(readList("guide_completed").toSet()) }
            var expenses by remember { mutableStateOf(readList("expenses")) }
            var interior by remember { mutableStateOf(readList("interior")) }
            var properties by remember { mutableStateOf(readList("properties")) }
            var menus by remember { mutableStateOf(readList("menus")) }
            var schedule by remember { mutableStateOf(readList("schedule")) }
            var taskDone by remember { mutableStateOf(readList("task_done").toSet()) }
            var draft by remember(page, tab) { mutableStateOf("") }
            var amount by remember(page, tab) { mutableStateOf("") }
            fun save(key: String, values: Collection<String>) {
                prefs.edit().putString(key, JSONArray(values.toList()).toString()).apply()
            }
            val doneCount = completed.count { it in roadmap }
            MaterialTheme(colorScheme = lightColorScheme(primary = forest, background = cream, surface = Color(0xFFFFFEFA))) {
                Scaffold(containerColor = cream, bottomBar = {
                    NavigationBar(containerColor = Color(0xFFFFFEFA), tonalElevation = 2.dp) {
                        listOf("홈", "할 일", "추가", "일정", "우리").forEachIndexed { i, label ->
                            NavigationBarItem(
                                selected = page == null && tab == i,
                                onClick = { tab = i; page = null },
                                icon = { Icon(listOf(Icons.Outlined.Home, Icons.Outlined.CheckCircle, Icons.Outlined.AddCircleOutline, Icons.Outlined.Event, Icons.Outlined.People)[i], contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFE0E9DB))
                            )
                        }
                    }
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                        Spacer(Modifier.height(22.dp))
                        if (page != null) {
                            TextButton(onClick = { page = null }) { Text("‹  홈으로", color = forest) }
                            Text(tiles.firstOrNull { it.destination == page }?.title ?: "카페 준비", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                        } else if (tab == 0) {
                            Text("OUR CAFÉ · WORKSPACE", color = forest, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(7.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("우리의 카페 만들기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFFE2EADD)) {
                                    Icon(Icons.Outlined.Favorite, contentDescription = "우리의 카페", tint = forest, modifier = Modifier.padding(12.dp).size(26.dp))
                                }
                            }
                            Text("오늘도 꿈에 한 걸음 가까워졌어요.", color = muted)
                            Spacer(Modifier.height(20.dp))
                            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                Column(Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("창업 준비 진행률", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("${doneCount * 100 / roadmap.size}%", color = forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(13.dp))
                                    LinearProgressIndicator(progress = { doneCount.toFloat() / roadmap.size }, modifier = Modifier.fillMaxWidth().height(9.dp), color = forest, trackColor = Color(0xFFE2E7DE))
                                    Spacer(Modifier.height(12.dp))
                                    Text("${roadmap.size}개 항목 중 ${doneCount}개 완료", color = muted)
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            tiles.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    pair.forEach { tile ->
                                        Card(onClick = { page = tile.destination }, modifier = Modifier.weight(1f).height(152.dp), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                                Surface(color = Color(0xFFE5EDE1), shape = RoundedCornerShape(15.dp)) {
                                                    Icon(tile.icon, contentDescription = null, tint = forest, modifier = Modifier.padding(10.dp).size(29.dp))
                                                }
                                                Text(tile.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text(tile.subtitle, color = muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp)) {
                                    Row {
                                        Text("이번 주 할 일", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("${tasks.count { it in taskDone }} / ${tasks.size} 완료", color = forest)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    if (tasks.isEmpty()) Text("할 일을 추가해 카페 준비를 시작해요.", color = muted)
                                    tasks.take(4).forEach { task ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = task in taskDone, onCheckedChange = { checked ->
                                                taskDone = if (checked) taskDone + task else taskDone - task
                                                save("task_done", taskDone)
                                            })
                                            Text(task, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    TextButton(onClick = { tab = 1 }) { Text("할 일 관리하기 ›") }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { page = "profit" }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Calculate, null)
                                Spacer(Modifier.width(8.dp))
                                Text("카페 수익·손익분기점 시뮬레이터")
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.People, contentDescription = null, tint = forest)
                                Spacer(Modifier.width(8.dp))
                                Text("함께 만드는 우리의 공간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("초대코드로 연결한 공동 공간은 ‘우리’ 탭에서 관리해요.", color = muted, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { tab = 4; page = null }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.People, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("우리의 공동 공간 열기")
                            }
                            Spacer(Modifier.height(10.dp))
                            CafeTools07()
                            Spacer(Modifier.height(20.dp))
                        } else {
                            Text("OUR CAFÉ · WORKSPACE", color = forest, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(7.dp))
                            Text(listOf("", "이번 주 할 일", "새 기록 추가", "우리의 일정", "우리의 공동 공간")[tab], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                        }
                        val destination = page ?: when(tab) { 1 -> "tasks"; 3 -> "schedule"; 4 -> "shared"; else -> "" }
                        when (destination) {
                            "roadmap" -> {
                                CafeGuide08(onOpen = { page = it }, completed = guideCompleted, onComplete = { title, checked ->
                                    guideCompleted = if (checked) guideCompleted + title else guideCompleted - title
                                    save("guide_completed", guideCompleted)
                                })
                                Spacer(Modifier.height(16.dp))
                                Text("기존 15단계 준비 체크리스트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                roadmap.forEach { stage ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = stage in completed, onCheckedChange = { checked ->
                                            completed = if (checked) completed + stage else completed - stage
                                            save("completed", completed)
                                        })
                                        Text(stage)
                                    }
                                }
                            }
                            "profit" -> CafeProfit08()
                            "startupcost" -> StartupCost09()
                            "contract" -> ContractChecklist09()
                            "scorer" -> PropertyScorer09()
                            "decisions" -> DecisionJournal09()
                            "competitors" -> CompetitorResearch10()
                            "quotes" -> VendorQuotes10()
                            "menuprofit" -> MenuProfit10()
                            "launch" -> LaunchReady10()
                            "shared" -> {
                                GoogleLoginPanel()
                                Spacer(Modifier.height(14.dp))
                                SharedCafePanel()
                                Spacer(Modifier.height(12.dp))
                                Text("공유 데이터는 위 공동 공간에서 관리됩니다. 다른 화면의 기록은 현재 이 휴대폰에 저장돼요.", style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                            "interior" -> InteriorBoard06()
                            "expenses" -> BudgetCalculator06(expenses)
                            "menus" -> MenuLab06()
                            "properties" -> PropertyCompare07()
                            "schedule" -> CafeCalendar07()
                            "tasks", "notes" -> {
                                val list = when(destination) {
                                    "tasks" -> tasks; "notes" -> notes; "expenses" -> expenses
                                    "interior" -> interior; "properties" -> properties; "menus" -> menus
                                    else -> schedule
                                }
                                if (destination == "expenses") {
                                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(18.dp)) {
                                            Text("누적 지출", color = muted)
                                            Text("${expenses.sumOf { it.substringBefore("|").toLongOrNull() ?: 0L }}원", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                                OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(),
                                    label = { Text(when(destination) {
                                        "tasks" -> "할 일"; "notes" -> "아이디어·회의 메모"; "expenses" -> "지출 항목"
                                        "interior" -> "인테리어 아이디어 또는 참고 링크"
                                        "properties" -> "매물 주소·보증금·특징"
                                        "menus" -> "메뉴명·레시피·원가"; else -> "일정 (날짜와 내용)"
                                    }) })
                                if (destination == "expenses") {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) },
                                        label = { Text("금액 (원)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(enabled = draft.isNotBlank() && (destination != "expenses" || amount.toLongOrNull() != null),
                                    onClick = {
                                        val value = if (destination == "expenses") "${amount}|${draft.trim()}" else draft.trim()
                                        val updated = list + value
                                        when(destination) {
                                            "tasks" -> { tasks = updated; save("tasks", tasks) }
                                            "notes" -> { notes = updated; save("notes", notes) }
                                            "expenses" -> { expenses = updated; save("expenses", expenses) }
                                            "interior" -> { interior = updated; save("interior", interior) }
                                            "properties" -> { properties = updated; save("properties", properties) }
                                            "menus" -> { menus = updated; save("menus", menus) }
                                            "schedule" -> { schedule = updated; save("schedule", schedule) }
                                        }
                                        draft = ""; amount = ""
                                    }) { Text("추가하기") }
                                Spacer(Modifier.height(10.dp))
                                if (list.isEmpty()) Text("아직 기록이 없어요. 첫 항목을 추가해 보세요.", color = muted)
                                list.forEachIndexed { index, value ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                            if (destination == "tasks") Checkbox(checked = value in taskDone, onCheckedChange = { checked ->
                                                taskDone = if (checked) taskDone + value else taskDone - value
                                                save("task_done", taskDone)
                                            })
                                            Text(if (destination == "expenses") "${value.substringAfter("|")} · ${value.substringBefore("|")}원" else value,
                                                modifier = Modifier.weight(1f), maxLines = 5)
                                            TextButton(onClick = {
                                                val updated = list.filterIndexed { i, _ -> i != index }
                                                when(destination) {
                                                    "tasks" -> { tasks = updated; save("tasks", tasks) }
                                                    "notes" -> { notes = updated; save("notes", notes) }
                                                    "expenses" -> { expenses = updated; save("expenses", expenses) }
                                                    "interior" -> { interior = updated; save("interior", interior) }
                                                    "properties" -> { properties = updated; save("properties", properties) }
                                                    "menus" -> { menus = updated; save("menus", menus) }
                                                    "schedule" -> { schedule = updated; save("schedule", schedule) }
                                                }
                                            }) { Text("삭제") }
                                        }
                                    }
                                }
                            }
                        }
                        if (page == null && tab == 2) {
                            Text("어떤 내용을 기록할까요?", color = muted)
                            Spacer(Modifier.height(12.dp))
                            tiles.drop(1).forEach { tile ->
                                Card(onClick = { page = tile.destination }, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
                                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(tile.icon, contentDescription = null, tint = forest, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Column { Text(tile.title, fontWeight = FontWeight.Bold); Text(tile.subtitle, color = muted, style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}
