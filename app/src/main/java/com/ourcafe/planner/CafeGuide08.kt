package com.ourcafe.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private data class GuideStep(
    val title: String, val period: String, val summary: String,
    val tasks: List<String>, val tips: List<String>, val warning: String = "",
    val destination: String? = null, val action: String = ""
)
private val guideSteps = listOf(
    GuideStep("창업 방향 설정", "1~2주", "우리 카페의 목표와 감당 가능한 위험을 먼저 정해요.",
        listOf("월 목표 순이익과 생활비 정하기", "자기자본·대출·예비자금 구분", "창업 이유와 역할 분담 기록"),
        listOf("인테리어보다 먼저 숫자와 고객을 정하세요.", "최소 3~6개월 운영 예비자금도 별도로 검토하세요."), destination = "expenses", action = "예산 계산기"),
    GuideStep("시장·상권 조사", "2~4주", "같은 동네라도 시간대별 손님과 경쟁 구도가 달라요.",
        listOf("평일·주말 오전·점심·저녁 유동인구 확인", "주변 카페 5곳 이상 가격·좌석·회전 관찰", "주민·직장인·학생 등 주고객층 정의"),
        listOf("소상공인 상권정보시스템 등 공개 자료와 직접 관찰을 함께 활용하세요.", "한 번 방문한 인상보다 여러 시간대의 기록이 중요해요."), destination = "properties", action = "매물 비교"),
    GuideStep("사업성·수익성 검증", "1~2주", "월 매출보다 고정비와 손익분기 주문 수가 중요해요.",
        listOf("객단가·일 주문 수·영업일 가정", "월세·인건비·관리비·기타 고정비 추정", "재료·포장·결제 수수료 등 변동비율 추정"),
        listOf("매출이 예상보다 20~30% 적어도 버틸 수 있는지 시험하세요.", "대표자 인건비·세금·대출이자는 따로 검토하세요."), destination = "profit", action = "수익 시뮬레이터"),
    GuideStep("점포 발굴·계약", "2~6주", "계약 전 영업 가능 여부를 확인해야 큰 손실을 피할 수 있어요.",
        listOf("건축물대장 용도·위반건축물 여부 확인", "등기부등본·소유자·근저당·권리관계 확인", "급배수·전기 용량·환기·소방·화장실 확인", "권리금·원상복구·임대료 인상 조건 검토"),
        listOf("임대차계약 전 관할 지자체에 해당 업종 영업 가능 여부를 확인하세요.", "필요한 인허가가 불가능할 때의 계약 처리 조건은 전문가와 협의하세요."),
        "법률·계약 조건은 매물마다 달라요. 계약서 서명 전 공인중개사·법률 전문가에게 확인하세요.", "properties", "매물 비교"),
    GuideStep("브랜드·공간 디자인", "2~4주", "멋진 사진뿐 아니라 고객 동선과 직원 작업 효율을 설계해요.",
        listOf("상호·상표 사용 가능 여부 확인", "좌석 수·대기 동선·제조 동선 그리기", "인테리어 견적 최소 3곳 비교", "공사 범위·추가 비용·하자보수 문서화"),
        listOf("청소·수납·배수·전기 콘센트 위치를 도면에 반영하세요.", "견적은 같은 공사 범위로 비교해야 의미가 있어요."), destination = "interior", action = "인테리어 보드"),
    GuideStep("시공·설비·장비", "4~8주", "장비 가격뿐 아니라 설치·수리·유지비를 확인해요.",
        listOf("에스프레소 머신·그라인더·제빙기·냉장고 용량 결정", "전기·급배수·환기·소방 공사 범위 확인", "납기·AS·소모품·설치비 비교", "공사 중간 점검과 인수 체크리스트 작성"),
        listOf("제빙기와 냉장고는 피크타임 수요를 기준으로 검토하세요.", "중고 장비는 상태·보증·수리 가능 여부를 확인하세요."), destination = "expenses", action = "장비 예산"),
    GuideStep("메뉴·운영 시스템", "2~4주", "초기에는 안정적인 핵심 메뉴와 일관된 품질이 우선이에요.",
        listOf("커피 5~7종·논커피 3~4종 등 후보 테스트", "레시피·제조시간·보관기간·폐기율 기록", "원재료·포장비·수수료 포함 원가 계산", "발주·재고·POS·직원 교육 방식 결정"),
        listOf("메뉴 수는 운영 인력과 장비에 맞게 조절하세요.", "인기 메뉴보다 재구매와 품질 일관성이 더 중요할 수 있어요."), destination = "menus", action = "메뉴 연구소"),
    GuideStep("인허가·오픈 준비", "1~3주", "영업 형태에 따라 신고·시설 기준이 달라질 수 있어요.",
        listOf("일반음식점·휴게음식점·제과점 등 해당 업종 확인", "관할 지자체 영업신고 요건 확인", "식품위생교육·건강진단 등 해당 의무 확인", "사업자등록·소방·가스·시설 기준 확인"),
        listOf("직접 제조·판매하는 품목과 운영 형태를 먼저 정리하세요.", "신고 순서와 구비서류는 관할 기관에서 최신 기준을 확인하세요."),
        "인허가 요건은 업종·시설·지역에 따라 달라요. 이 가이드는 허가 가능 여부를 보장하지 않습니다."),
    GuideStep("가오픈·정식 오픈", "1~2주", "실제 주문 흐름을 시험하고 문제를 고쳐요.",
        listOf("지인·소규모 고객으로 가오픈 테스트", "주문 대기시간·맛 편차·동선 점검", "지도·영업시간·메뉴판·고객 안내 점검", "오픈 후 첫 2주 고객 피드백 기록"),
        listOf("초기에는 할인보다 안정적인 운영과 응대 품질을 챙기세요.", "불만·재방문 이유를 구체적으로 기록하세요."), destination = "schedule", action = "오픈 일정"),
    GuideStep("매출 최적화·확장", "오픈 이후", "매출뿐 아니라 남는 돈과 재방문을 추적해요.",
        listOf("주간 매출·객단가·시간대별 판매량 기록", "폐기율·재고·인건비·고정비 점검", "재방문 고객 의견과 리뷰 분석", "수익성 확인 후 메뉴·운영시간 조정"),
        listOf("매출이 늘어도 비용이 더 빨리 늘면 수익이 줄 수 있어요.", "확장은 현금흐름이 안정된 뒤 검토하세요."), destination = "profit", action = "수익 시뮬레이터")
)
private val decisions = listOf(
    "사업 목표" to "월 순이익 목표 · 운영 기간 · 확장 여부",
    "창업 예산" to "자기자본 · 대출 · 예비자금",
    "지역" to "도시 · 동네 · 주거/상업/관광 상권",
    "고객층" to "직장인 · 학생 · 연인 · 가족 · 관광객",
    "카페 유형" to "테이크아웃 · 동네 · 감성 · 디저트",
    "규모" to "평수 · 좌석 · 주차 · 테라스",
    "부동산" to "보증금 · 월세 · 관리비 · 권리금",
    "브랜드" to "이름 · 로고 · 색상 · 차별점",
    "인테리어" to "공간 스타일 · 조명 · 가구 · 바 동선",
    "메뉴" to "커피 · 시그니처 · 디저트 · 가격",
    "운영" to "영업시간 · 직원 · 발주 · 재고 · POS",
    "마케팅" to "지도 · SNS · 리뷰 · 재방문"
)
private val scores = listOf(
    "목표 고객과 상권의 일치" to 20,
    "실제 유동인구·방문 수요" to 20,
    "임대료 대비 예상 매출" to 20,
    "접근성·주차·가시성" to 15,
    "경쟁업체와 차별화 가능성" to 10,
    "건물 상태·설비·인허가" to 10,
    "향후 개발·상권 변화" to 5
)

@Composable
fun CafeGuide08(onOpen: (String) -> Unit, completed: Set<String>, onComplete: (String, Boolean) -> Unit) {
    var section by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableIntStateOf(0) }
    val done = guideSteps.count { it.title in completed }
    Text("카페 창업 실전 가이드", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("읽고, 조사하고, 체크하면서 한 단계씩 준비해요.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(progress = { done.toFloat() / guideSteps.size }, modifier = Modifier.fillMaxWidth())
    Text("가이드 단계 완료: $done / ${guideSteps.size}", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(12.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("전체 로드맵", "핵심 결정", "상권 점수").forEachIndexed { i, label ->
            SegmentedButton(selected = section == i, onClick = { section = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label, maxLines = 1) }
        }
    }
    Spacer(Modifier.height(12.dp))
    when (section) {
        0 -> guideSteps.forEachIndexed { i, step ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = step.title in completed, onCheckedChange = { onComplete(step.title, it) })
                        Column(Modifier.weight(1f)) {
                            Text("${i + 1}. ${step.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("예상 ${step.period} · ${step.summary}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { expanded = if (expanded == i) -1 else i }) {
                            Icon(if (expanded == i) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, "상세 보기")
                        }
                    }
                    if (expanded == i) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Text("확인할 일", fontWeight = FontWeight.Bold)
                        step.tasks.forEach { Text("□ $it", modifier = Modifier.padding(top = 6.dp)) }
                        Spacer(Modifier.height(12.dp))
                        Text("실전 팁", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        step.tips.forEach { Text("• $it", modifier = Modifier.padding(top = 5.dp)) }
                        if (step.warning.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text("주의: ${step.warning}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        step.destination?.let { destination ->
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { onOpen(destination) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.OpenInNew, null)
                                Spacer(Modifier.width(6.dp))
                                Text(step.action)
                            }
                        }
                    }
                }
            }
        }
        1 -> {
            Text("결정해야 할 핵심 12가지", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            decisions.forEachIndexed { i, pair ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${i + 1}. ${pair.first}", fontWeight = FontWeight.Bold)
                        Text(pair.second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("이 항목들은 한 번에 정답을 고르는 목록이 아니라, 조사 결과에 따라 수정하는 의사결정 기준이에요.")
        }
        2 -> {
            Text("상권·매물 평가표 (100점)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("각 항목의 점수는 비교를 위한 예시 가중치예요. 실제 영업 가능성과 예산을 우선 확인하세요.", style = MaterialTheme.typography.bodySmall)
            scores.forEach { (name, score) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                    Text(name, modifier = Modifier.weight(1f))
                    Text("${score}점", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(10.dp))
            Text("점수가 높아도 법적으로 영업할 수 없거나 예산을 초과하면 계약하지 마세요.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { onOpen("properties") }, modifier = Modifier.fillMaxWidth()) { Text("후보 매물 기록하기") }
        }
    }
}

@Composable
fun CafeProfit08() {
    var orders by remember { mutableFloatStateOf(90f) }
    var ticket by remember { mutableFloatStateOf(6500f) }
    var rent by remember { mutableFloatStateOf(180f) }
    var payroll by remember { mutableFloatStateOf(350f) }
    var fixedOther by remember { mutableFloatStateOf(80f) }
    var variable by remember { mutableFloatStateOf(32f) }
    var days by remember { mutableFloatStateOf(30f) }
    val revenue = orders.toDouble() * ticket.toDouble() * days.toDouble()
    val variableCost = revenue * variable / 100
    val fixed = (rent + payroll + fixedOther).toDouble() * 10000
    val operating = revenue - variableCost - fixed
    val contribution = ticket * (1 - variable / 100)
    val breakeven = if (contribution > 0) kotlin.math.ceil(fixed / (contribution * days)).toLong() else 0L
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    Text("카페 수익 시뮬레이터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("숫자를 움직여 손익분기점과 월 예상 영업손익을 비교해요.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("월 예상 영업손익", fontWeight = FontWeight.Bold)
            Text("${fmt.format(operating.toLong())}원", style = MaterialTheme.typography.headlineMedium, color = if (operating >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("월매출 ${fmt.format(revenue.toLong())}원")
            Text("변동비 ${fmt.format(variableCost.toLong())}원 · 고정비 ${fmt.format(fixed.toLong())}원")
            Text("손익분기 주문 수: 하루 약 ${fmt.format(breakeven)}건", fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(12.dp))
    @Composable fun slider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, suffix: String, update: (Float) -> Unit) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("${value.toInt()}$suffix")
        }
        Slider(value = value, onValueChange = update, valueRange = range)
    }
    slider("하루 주문 건수", orders, 0f..300f, "건") { orders = it }
    slider("평균 객단가", ticket, 2000f..20000f, "원") { ticket = it }
    slider("월 영업일", days, 15f..31f, "일") { days = it }
    slider("월세 + 관리비", rent, 0f..1000f, "만원") { rent = it }
    slider("월 인건비", payroll, 0f..1500f, "만원") { payroll = it }
    slider("기타 월 고정비", fixedOther, 0f..800f, "만원") { fixedOther = it }
    slider("재료·포장·결제 등 변동비율", variable, 0f..80f, "%") { variable = it }
    Text("가상 시뮬레이션입니다. 세금·감가상각·대출 원리금·대표자 보수 등은 별도 반영해야 하며 실제 수익을 보장하지 않아요.", style = MaterialTheme.typography.bodySmall)
}
