package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
private data class SharedEntry(val id: String, val text: String, val amount: Long = 0L)

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
    var price by remember(userId) { mutableStateOf("") }
    var entries by remember(userId) { mutableStateOf(emptyList<SharedEntry>()) }
    var completed by remember(userId) { mutableStateOf(emptySet<String>()) }

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
            val kind = listOf("", "notes", "tasks", "expenses")[section]
            val registration = db.collection("rooms").document(id).collection(kind).addSnapshotListener { snapshot, ex ->
                if (ex != null) error = ex.localizedMessage
                else entries = snapshot?.documents?.map { SharedEntry(it.id, it.getString("text") ?: "", it.getLong("amount") ?: 0L) } ?: emptyList()
            }
            onDispose { registration.remove() }
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
                    val id = invite.getString("roomId") ?: error("초대코드를 찾지 못했어요.")
                    db.collection("rooms").document(id).update(mapOf("members" to FieldValue.arrayUnion(userId), "joinCode" to typedCode)).await()
                    db.collection("users").document(userId).set(mapOf("roomId" to id)).await()
                }
            }) { Text("초대코드로 참여하기") }
        } else {
            Text(roomName, style = MaterialTheme.typography.titleMedium)
            Text("상대방에게 이 초대코드를 알려주세요: $code")
            Text("코드는 8자리이며, Google 계정으로 로그인한 상대방이 입력하면 참여할 수 있어요.", style = MaterialTheme.typography.bodySmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("로드맵", "메모", "할 일", "예산").forEachIndexed { i, label ->
                    SegmentedButton(selected = section == i, onClick = { section = i; draft = ""; price = ""; entries = emptyList() }, shape = SegmentedButtonDefaults.itemShape(index = i, count = 4)) { Text(label) }
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
            } else {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text(when (section) { 1 -> "공유 메모"; 2 -> "공유 할 일"; else -> "지출 항목" }) }, modifier = Modifier.fillMaxWidth())
                if (section == 3) OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("금액 (원)") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && draft.isNotBlank() && (section != 3 || price.toLongOrNull() != null), onClick = {
                    val kind = listOf("", "notes", "tasks", "expenses")[section]
                    val entry = mapOf("text" to draft.trim(), "amount" to (if (section == 3) price.toLongOrNull() ?: 0L else 0L), "createdAt" to FieldValue.serverTimestamp())
                    perform {
                        db.collection("rooms").document(roomId!!).collection(kind).add(entry).await()
                        draft = ""; price = ""
                    }
                }) { Text("공동 공간에 추가") }
                if (section == 3) Text("공동 지출 합계: ${entries.sumOf { it.amount }}원")
                entries.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.text + if (section == 3) " · ${entry.amount}원" else "", modifier = Modifier.weight(1f))
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
