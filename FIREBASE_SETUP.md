# OUR CAFÉ — Firebase Google 로그인 연결

## 1. Firebase 프로젝트 생성
https://console.firebase.google.com/ 에서 새 프로젝트를 만듭니다.
Android 앱을 등록할 때 패키지 이름은 정확히 `com.ourcafe.planner` 입니다.

## 2. Google 로그인 활성화
Firebase Console → Authentication → Sign-in method → Google 사용 설정.
프로젝트 설정 → Android 앱에 **디버그 APK 서명 SHA-1**을 등록합니다.

GitHub Actions의 기본 debug keystore는 매 실행 새로 생성될 수 있으므로, 안정적인 Google 로그인을 위해 **고정된 서명키를 안전하게 관리하는 별도 빌드 작업**이 필요합니다. 아직 고정 서명 설정은 구현되지 않았습니다. 지금은 OAuth SHA-1 등록 전까지 로그인 성공을 기대하지 마세요.

## 3. google-services.json 설정
Google 로그인 활성화 이후 새 `google-services.json`을 다운로드합니다.
공개 GitHub 저장소에 직접 커밋하지 마세요. 파일 내용은 비밀 키 자체는 아니지만 설정 정보를 저장소에 노출하지 않도록 CI Secret으로 관리합니다.

GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret.
이름: `FIREBASE_CONFIG_BASE64`
값: 다운로드한 JSON 파일을 **base64로 인코딩한 문자열**.
CI는 이 Secret이 있으면 빌드 시 `app/google-services.json`을 복원합니다.
Secret이 없으면 기존 로컬 전용 앱이 계속 빌드되고 Google 로그인은 비활성 상태입니다.

## 4. 아직 남은 작업
- 고정 서명키 생성/안전한 CI 관리 및 SHA-1 등록
- 두 사람만 참여 가능한 Firestore 워크스페이스와 보안 규칙
- 기존 로컬 메모를 클라우드로 이전하는 안전한 절차
- 두 기기 실시간 동기화 및 충돌 처리

**주의:** Google 로그인 구현만으로 두 사람의 기록이 공유되지는 않습니다. 기존 데이터는 아직 각 기기에 로컬 저장됩니다.
