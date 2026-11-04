package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class TogetherEntry17(
    val id: String, val text: String, val owner: String, val amount: Long,
    val status: String, val votes: Map<String, String>
)

@Composable
fun TogetherStudio17(roomId: String) {
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: return
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val kinds = listOf("담당 업무", "커플 투표", "공동 예산", "활동 기록")
    val collections = listOf("assignments17", "votes17", "budget17", "activity17")
    var tab by remember { mutableIntStateOf(0) }
    var rows by remember(roomId, tab) { mutableStateOf(emptyList<TogetherEntry17>()) }
    var text by remember(tab) { mutableStateOf("") }
    var amount by remember(tab) { mutableStateOf("") }
    var assignedTo by remember(tab) { mutableStateOf(uid) }
    var members by remember(roomId) { mutableStateOf(listOf(uid)) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(roomId) {
        val registration = db.collection("rooms").document(roomId).addSnapshotListener { snap, ex ->
            if (ex != null) error = ex.localizedMessage
            else members = (snap?.get("members") as? List<*>)?.filterIsInstance<String>() ?: listOf(uid)
        }
        onDispose { registration.remove() }
    }
    DisposableEffect(roomId, tab) {
        val registration = db.collection("rooms").document(roomId).collection(collections[tab])
            .addSnapshotListener { snap, ex ->
                if (ex != null) error = ex.localizedMessage
                else rows = snap?.documents?.map { doc ->
                    TogetherEntry17(doc.id, doc.getString("text") ?: "",
                        doc.getString("owner") ?: "", doc.getLong("amount") ?: 0L,
                        doc.getString("status") ?: "진행 전",
                        (doc.get("votes") as? Map<*, *>)?.entries?.mapNotNull {
                            val key = it.key as? String
                            val value = it.value as? String
                            if (key != null && value != null) key to value else null
                        }?.toMap() ?: emptyMap())
                } ?: emptyList()
            }
        onDispose { registration.remove() }
    }
    fun write(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { action() } catch (ex: Exception) { error = ex.localizedMessage ?: "저장 실패" }
            finally { busy = false }
        }
    }
    val base = db.collection("rooms").document(roomId)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("TOGETHER 2.0 · 공동 창업", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("공동 공간 구성원만 읽고 수정할 수 있는 Firebase 실시간 기록입니다.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            kinds.forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        if (error != null) Text("오류: $error", color = MaterialTheme.colorScheme.error)
        if (tab != 3) {
            OutlinedTextField(text, { text = it }, label = { Text(when (tab) {
                0 -> "할 일"; 1 -> "투표할 안건"; else -> "지출 내역"
            }) }, modifier = Modifier.fillMaxWidth())
            if (tab == 0) {
                Text("담당자", fontWeight = FontWeight.Bold)
                members.forEachIndexed { i, member ->
                    Row {
                        RadioButton(selected = assignedTo == member, onClick = { assignedTo = member })
                        Text("구성원 ${i + 1} (${member.take(6)}…)", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            if (tab == 2) OutlinedTextField(amount, { amount = it.filter(Char::isDigit) },
                label = { Text("금액 (원)") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && text.isNotBlank() && (tab != 2 || amount.toLongOrNull() != null), onClick = {
                val kind = collections[tab]
                val title = text.trim()
                write {
                    val doc = base.collection(kind).document()
                    doc.set(mapOf("text" to title, "owner" to (if (tab == 0) assignedTo else uid),
                        "amount" to (amount.toLongOrNull() ?: 0L), "status" to "진행 전",
                        "votes" to emptyMap<String, String>(), "createdAt" to FieldValue.serverTimestamp())).await()
                    base.collection("activity17").add(mapOf("text" to "${kinds[tab]} 추가: $title",
                        "owner" to uid, "amount" to 0L, "status" to "기록",
                        "votes" to emptyMap<String, String>(), "createdAt" to FieldValue.serverTimestamp())).await()
                    text = ""; amount = ""
                }
            }) { Text("공동 기록 추가") }
        }
        if (tab == 2) Text("공동 지출 합계: ${rows.sumOf { it.amount }}원", fontWeight = FontWeight.Bold)
        rows.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.text, fontWeight = FontWeight.Bold)
                    if (tab == 0) {
                        Text("담당: ${members.indexOf(item.owner).let { if (it >= 0) "구성원 ${it + 1}" else "미지정" }} · ${item.status}")
                        Row {
                            listOf("진행 전", "진행 중", "완료").forEach { status ->
                                TextButton(enabled = !busy, onClick = {
                                    write {
                                        base.collection("assignments17").document(item.id).update("status", status).await()
                                        base.collection("activity17").add(mapOf("text" to "업무 상태 변경: ${item.text} → $status",
                                            "owner" to uid, "amount" to 0L, "status" to "기록",
                                            "votes" to emptyMap<String, String>(), "createdAt" to FieldValue.serverTimestamp())).await()
                                    }
                                }) { Text(status, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                    if (tab == 1) {
                        Text("찬성 ${item.votes.values.count { it == "yes" }} · 반대 ${item.votes.values.count { it == "no" }}")
                        Row {
                            listOf("yes" to "찬성", "no" to "반대").forEach { (vote, label) ->
                                TextButton(enabled = !busy, onClick = {
                                    write {
                                        base.collection("votes17").document(item.id).update("votes.$uid", vote).await()
                                    }
                                }) { Text(label + if (item.votes[uid] == vote) " ✓" else "") }
                            }
                        }
                    }
                    if (tab == 2) Text("지출 ${item.amount}원 · 입력자 ${item.owner.take(6)}…")
                    if (tab != 3) TextButton(enabled = !busy, onClick = {
                        write {
                            base.collection(collections[tab]).document(item.id).delete().await()
                            base.collection("activity17").add(mapOf("text" to "${kinds[tab]} 삭제: ${item.text}",
                                "owner" to uid, "amount" to 0L, "status" to "기록",
                                "votes" to emptyMap<String, String>(), "createdAt" to FieldValue.serverTimestamp())).await()
                        }
                    }) { Text("삭제") }
                }
            }
        }
        if (tab == 3) Text("활동 기록은 감사용으로 삭제할 수 없습니다. 시간순 정렬은 추후 개선 예정입니다.", style = MaterialTheme.typography.bodySmall)
    }
}
