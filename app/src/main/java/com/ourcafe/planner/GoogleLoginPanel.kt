package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GoogleLoginPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (BuildConfig.FIREBASE_ENABLED) {
            userName = FirebaseAuth.getInstance().currentUser?.displayName
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("우리의 공동 공간", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (!BuildConfig.FIREBASE_ENABLED) {
                Text("Google 로그인 준비 완료 · Firebase 연결 대기 중", style = MaterialTheme.typography.bodySmall)
                Text("Firebase 설정 후 로그인 버튼이 활성화됩니다.", style = MaterialTheme.typography.bodySmall)
            } else if (FirebaseAuth.getInstance().currentUser != null) {
                Text("로그인: ${userName ?: FirebaseAuth.getInstance().currentUser?.email ?: "Google 사용자"}")
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    userName = null
                    error = null
                }) { Text("로그아웃") }
            } else {
                Button(enabled = !busy, onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                            require(id != 0) { "Firebase OAuth 웹 클라이언트 ID가 없습니다." }
                            val googleOption = GetGoogleIdOption.Builder()
                                .setServerClientId(context.getString(id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleOption)
                                .build()
                            val credential = CredentialManager.create(context).getCredential(
                                context = context, request = request
                            ).credential
                            require(credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) { "Google 로그인 응답을 확인할 수 없습니다." }
                            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                            val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                            val result = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                            userName = result.user?.displayName ?: result.user?.email
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "로그인에 실패했습니다."
                        } finally {
                            busy = false
                        }
                    }
                }) { Text(if (busy) "로그인 중..." else "Google 계정으로 로그인") }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Text("공동 데이터 동기화는 다음 단계에서 연결됩니다.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
