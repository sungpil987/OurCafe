package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

private val cafeStages = listOf("창업 예산 정하기","카페 콘셉트 정하기","고객층 정의하기","경쟁 카페 조사","상권 분석하기","손익분기점 계산","매물 후보 비교","임대차 계약 검토","브랜드 이름과 로고","인테리어 견적 비교","커피 장비 선정","메뉴와 원가표","인허가 준비","가오픈 테스트","정식 오픈")
private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private fun inviteCode(): String = (1..8).map { alphabet[SecureRandom().nextInt(alphabet.length)] }.joinToString("")
private data class SharedEntry(val id: String, val text: String, val amount: Long = 0L, val date: String = "", val done: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCafePanel() {
    if (!BuildConfig.FIREBASE_ENABLED) return
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var uid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { uid = it.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    if (uid == null) {
        Text("공동 공간을 사용하려면 Google 계정으로 로그인해 주세요.")
        return
    }
    val userId = uid!!
    val scope = rememberCoroutineScope()
    var roomId by remember(userId) { mutableStateOf<String?>(null) }
    var roomName by remember(userId) { mutableStateOf("우리의 카페") }
    var code by remember(userId) { mutableStateOf("") }
    var typedCode by remember(userId) { mutableStateOf("") }
    var busy by remember(userId) { mutableStateOf(false) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    var section by remember(userId) { mutableIntStateOf(0) }
    var draft by remember(userId) { mutableStateOf("") }
    var editingEntryId by remember(userId) { mutableStateOf<String?>(null) }
    var price by remember(userId) { mutableStateOf("") }
    var entries by remember(userId) { mutableStateOf(emptyList<SharedEntry>()) }
    var month by remember(userId) { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember(userId) { mutableStateOf(LocalDate.now()) }
    var eventDate by remember(userId) { mutableStateOf(LocalDate.now().toString()) }
    var eventNote by remember(userId) { mutableStateOf("") }
    var sharedTaskTotal by remember(userId) { mutableIntStateOf(0) }
    var sharedTaskDone by remember(userId) { mutableIntStateOf(0) }
    var sharedExpenseTotal by remember(userId) { mutableLongStateOf(0L) }
    var sharedNoteTotal by remember(userId) { mutableIntStateOf(0) }
    var sharedEventTotal by remember(userId) { mutableIntStateOf(0) }
    var completed by remember(userId) { mutableStateOf(emptySet<String>()) }
    var showTogether17 by remember(userId) { mutableStateOf(false) }

    DisposableEffect(userId) {
        val registration = db.collection("users").document(userId).addSnapshotListener { snapshot, ex ->
            if (ex != null) error = ex.localizedMessage
            else roomId = snapshot?.getString("roomId")
        }
        onDispose { registration.remove() }
    }
    DisposableEffect(roomId) {
        val id = roomId
        if (id == null) onDispose { } else {
            val registration = db.collection("rooms").document(id).addSnapshotListener { snapshot, ex ->
                if (ex != null) error = ex.localizedMessage
                else if (snapshot != null && snapshot.exists()) {
                    roomName = snapshot.getString("name") ?: "우리의 카페"
                    code = snapshot.getString("inviteCode") ?: ""
                    completed = (snapshot.get("completed") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                }
            }
            onDispose { registration.remove() }
        }
    }
    DisposableEffect(roomId, section) {
        val id = roomId
        if (id == null || section == 0) onDispose { } else {
            val kind = listOf("", "notes", "tasks", "expenses", "events")[section]
            val registration = db.collection("rooms").document(id).collection(kind).addSnapshotListener { snapshot, ex ->
                if (ex != null) error = ex.localizedMessage
                else entries = snapshot?.documents?.map { SharedEntry(it.id, it.getString("text") ?: "", it.getLong("amount") ?: 0L, it.getString("date") ?: "", it.getBoolean("done") ?: false) } ?: emptyList()
            }
            onDispose { registration.remove() }
        }
    }
    DisposableEffect(roomId) {
        val id = roomId
        if (id == null) onDispose { } else {
            val registrations = listOf("tasks", "expenses", "notes", "events").map { kind ->
                db.collection("rooms").document(id).collection(kind).addSnapshotListener { snapshot, ex ->
                    if (ex != null) { error = ex.localizedMessage }
                    else {
                        val docs = snapshot?.documents.orEmpty()
                        when (kind) {
                            "tasks" -> {
                                sharedTaskTotal = docs.size
                                sharedTaskDone = docs.count { it.getBoolean("done") == true }
                            }
                            "expenses" -> sharedExpenseTotal = docs.sumOf { it.getLong("amount") ?: 0L }
                            "notes" -> sharedNoteTotal = docs.size
                            "events" -> sharedEventTotal = docs.size
                        }
                    }
                }
            }
            onDispose { registrations.forEach { it.remove() } }
        }
    }
    fun perform(action: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try { action() } catch (ex: Exception) { error = ex.localizedMessage ?: "작업에 실패했어요." }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("☕ 커플 공동 공간", style = MaterialTheme.typography.titleLarge)
        if (roomId == null) {
            Text("공동 공간을 만들거나 상대방의 초대코드로 참여하세요.")
            Button(enabled = !busy, onClick = {
                perform {
                    val newCode = inviteCode()
                    val room = db.collection("rooms").document()
                    room.set(mapOf("name" to "우리의 카페", "ownerUid" to userId, "members" to listOf(userId), "inviteCode" to newCode, "completed" to emptyList<String>())).await()
                    db.collection("invites").document(newCode).set(mapOf("roomId" to room.id)).await()
                    db.collection("users").document(userId).set(mapOf("roomId" to room.id)).await()
                }
            }) { Text("우리 공동 공간 만들기") }
            OutlinedTextField(value = typedCode, onValueChange = { typedCode = it.uppercase().filter { c -> c in alphabet }.take(8) }, label = { Text("상대방 초대코드 8자리") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && typedCode.length == 8, onClick = {
                perform {
                    val invite = db.collection("invites").document(typedCode).get().await()
                    val id = invite.getString("roomId") ?: throw IllegalArgumentException("초대코드를 찾지 못했어요.")
                    db.collection("rooms").document(id).update(mapOf("members" to FieldValue.arrayUnion(userId), "joinCode" to typedCode)).await()
                    db.collection("users").document(userId).set(mapOf("roomId" to id)).await()
                }
            }) { Text("초대코드로 참여하기") }
        } else {
            Text(roomName, style = MaterialTheme.typography.titleMedium)
            Text("상대방에게 이 초대코드를 알려주세요: $code")
            Text("코드는 8자리이며, Google 계정으로 로그인한 상대방이 입력하면 참여할 수 있어요.", style = MaterialTheme.typography.bodySmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("우리의 공동 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("로드맵 ${completed.size}/15 · 할 일 ${sharedTaskDone}/${sharedTaskTotal} 완료")
                    LinearProgressIndicator(progress = { completed.size.coerceIn(0, 15) / 15f }, modifier = Modifier.fillMaxWidth())
                    Text("공유 메모 ${sharedNoteTotal}개 · 공유 일정 ${sharedEventTotal}개")
                    Text("공동 지출 합계 ${"%,d".format(sharedExpenseTotal)}원", fontWeight = FontWeight.Bold)
                    Text("공동 공간에 저장한 기록 기준이며 변경 사항은 실시간 반영됩니다.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedButton(onClick = { showTogether17 = !showTogether17 }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showTogether17) "TOGETHER 2.0 접기" else "TOGETHER 2.0 · 담당자·투표·공동 예산·활동 기록")
            }
            if (showTogether17) TogetherStudio17(roomId!!)

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("로드맵", "메모", "할 일", "예산", "일정").forEachIndexed { i, label ->
                    FilterChip(selected = section == i, onClick = {
                        section = i; draft = ""; price = ""; editingEntryId = null; entries = emptyList()
                    }, label = { Text(label) })
                }
            }
            if (section == 0) {
                Text("공동 진행률: ${completed.size} / ${cafeStages.size}")
                cafeStages.forEach { stage ->
                    Row {
                        Checkbox(checked = stage in completed, enabled = !busy, onCheckedChange = { checked ->
                            perform {
                                db.collection("rooms").document(roomId!!).update("completed", if (checked) FieldValue.arrayUnion(stage) else FieldValue.arrayRemove(stage)).await()
                            }
                        })
                        Text(stage, modifier = Modifier.padding(top = 13.dp))
                    }
                }
            } else if (section == 4) {
                Text("공유 월간 달력", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹ 이전") }
                    Text("${month.year}년 ${month.monthValue}월", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { month = month.plusMonths(1) }) { Text("다음 ›") }
                }
                val offset = month.atDay(1).dayOfWeek.value % 7
                val cellCount = ((offset + month.lengthOfMonth() + 6) / 7) * 7
                Row(Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach { Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall) }
                }
                repeat(cellCount / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val number = week * 7 + column - offset + 1
                            val day = if (number in 1..month.lengthOfMonth()) month.atDay(number) else null
                            val dateString = day?.toString() ?: ""
                            val dayEntries = entries.filter { it.date == dateString }
                            Surface(
                                onClick = { if (day != null) { selectedDate = day; eventDate = day.toString() } },
                                enabled = day != null,
                                color = if (day == selectedDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(78.dp).padding(2.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    Text(if (day == null) "" else number.toString(), style = MaterialTheme.typography.labelSmall)
                                    dayEntries.take(2).forEach { event ->
                                        Text("• ${event.text}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                    if (dayEntries.size > 2) Text("+${dayEntries.size - 2}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Text("${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 일정", fontWeight = FontWeight.Bold)
                entries.filter { it.date == selectedDate.toString() }.forEach { event ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(event.text, modifier = Modifier.weight(1f))
                        TextButton(enabled = !busy, onClick = {
                            editingEntryId = event.id
                            draft = event.text
                            eventDate = event.date
                            eventNote = ""
                        }) { Text("수정") }
                        TextButton(enabled = !busy, onClick = {
                            perform { db.collection("rooms").document(roomId!!).collection("events").document(event.id).delete().await() }
                        }) { Text("삭제") }
                    }
                }
                OutlinedTextField(value = eventDate, onValueChange = { eventDate = it.take(10) },
                    label = { Text("날짜 (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = eventNote, onValueChange = { eventNote = it }, label = { Text("시간·장소·메모") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && draft.isNotBlank() && runCatching { LocalDate.parse(eventDate) }.isSuccess, onClick = {
                    val title = draft.trim()
                    val date = eventDate
                    val detail = eventNote.trim()
                    perform {
                        val collection = db.collection("rooms").document(roomId!!).collection("events")
                        val fields = mapOf("text" to (if (detail.isBlank()) title else "$title · $detail"), "date" to date)
                        if (editingEntryId == null) collection.add(fields + mapOf("createdAt" to FieldValue.serverTimestamp())).await()
                        else collection.document(editingEntryId!!).update(fields).await()
                        draft = ""; eventNote = ""; editingEntryId = null
                    }
                }) { Text(if (editingEntryId == null) "공유 일정 저장" else "일정 수정 저장") }
                Text("두 사람의 공동 공간에 저장한 일정은 실시간으로 동기화돼요.", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text(when (section) { 1 -> "공유 메모"; 2 -> "공유 할 일"; else -> "지출 항목" }) }, modifier = Modifier.fillMaxWidth())
                if (section == 3) OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("금액 (원)") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && draft.isNotBlank() && (section != 3 || price.toLongOrNull() != null), onClick = {
                    val kind = listOf("", "notes", "tasks", "expenses")[section]
                    val entry = mapOf("text" to draft.trim(), "amount" to (if (section == 3) price.toLongOrNull() ?: 0L else 0L), "createdAt" to FieldValue.serverTimestamp())
                    perform {
                        val collection = db.collection("rooms").document(roomId!!).collection(kind)
                        if (editingEntryId == null) collection.add(entry).await()
                        else collection.document(editingEntryId!!).update(mapOf("text" to draft.trim(), "amount" to (if (section == 3) price.toLongOrNull() ?: 0L else 0L))).await()
                        draft = ""; price = ""; editingEntryId = null
                    }
                }) { Text(if (editingEntryId == null) "공동 공간에 추가" else "공동 기록 수정 저장") }
                if (section == 3) Text("공동 지출 합계: ${entries.sumOf { it.amount }}원")
                entries.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (section == 2) Checkbox(checked = entry.done, enabled = !busy, onCheckedChange = { done ->
                            perform { db.collection("rooms").document(roomId!!).collection("tasks").document(entry.id).update("done", done).await() }
                        })
                        Text(entry.text + if (section == 3) " · ${entry.amount}원" else "", modifier = Modifier.weight(1f),
                            color = if (entry.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        TextButton(enabled = !busy, onClick = {
                            editingEntryId = entry.id
                            draft = entry.text
                            price = if (section == 3) entry.amount.toString() else ""
                        }) { Text("수정") }
                        TextButton(enabled = !busy, onClick = {
                            val kind = listOf("", "notes", "tasks", "expenses")[section]
                            perform { db.collection("rooms").document(roomId!!).collection(kind).document(entry.id).delete().await() }
                        }) { Text("삭제") }
                    }
                }
            }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (error != null) Text("오류: $error", color = MaterialTheme.colorScheme.error)
    }
}
