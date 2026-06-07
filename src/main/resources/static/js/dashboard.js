let currentTradeId = null;
let currentPaymentId = null;
const eduModal = new bootstrap.Modal(document.getElementById('eduModal'));

const tokens = {
    'user1': 'header.eyJ1c2VySWQiOjF9.signature',
    'user2': 'header.eyJ1c2VySWQiOjJ9.signature',
    'hacker': 'header.eyJ1c2VySWQiOjk5OX0.signature'
};

function getSelectedToken() {
    return tokens[document.getElementById('userSelector').value];
}

function toggleHackerMode() {
    const isChecked = document.getElementById('hackerModeToggle').checked;
    const sections = document.querySelectorAll('.hacker-mode-section');
    sections.forEach(s => s.style.display = isChecked ? 'block' : 'none');
}

async function callApi(method, url, body = null) {
    const token = getSelectedToken();
    const headers = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);

    const startTime = Date.now();
    try {
        const response = await fetch(url, options);
        const data = await response.json();
        const duration = Date.now() - startTime;

        appendToApiInspector(method, url, body, data, response.status, duration);
        return { status: response.status, data };
    } catch (error) {
        appendToApiInspector(method, url, body, { error: error.message }, 500, 0);
        return { status: 500, data: { message: error.message } };
    }
}

function appendToApiInspector(method, url, request, response, status, duration) {
    const container = document.getElementById('apiInspectorContent');
    const entry = document.createElement('div');
    entry.className = "mb-4 pb-3 border-bottom";
    entry.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="badge ${status < 400 ? 'bg-success' : 'bg-danger'}">${method} ${status}</span>
            <small class="text-muted">${url} (${duration}ms)</small>
        </div>
        <div class="api-inspector-code mb-2">
            <strong>요청 본문 (Request Body):</strong><br>${request ? JSON.stringify(request, null, 2) : '없음'}<br><br>
            <strong>응답 결과 (Response):</strong><br>${JSON.stringify(response, null, 2)}
        </div>
    `;
    container.appendChild(entry);
}

function clearModal() {
    document.getElementById('userExperienceContent').innerHTML = '<div class="spinner-border text-primary" role="status"></div><p class="mt-2">시퀀스 처리 중...</p>';
    document.getElementById('apiInspectorContent').innerHTML = '';
    document.getElementById('securityInsightContent').innerHTML = '<div class="alert alert-info">이 단계에서 발견된 보안 이슈가 없습니다.</div>';
    
    // 탭 초기화
    document.getElementById('pills-user-tab').click();
}

function setSecurityInsight(title, description, cwe, isExploited = false) {
    const container = document.getElementById('securityInsightContent');
    container.innerHTML = `
        <div class="card security-insight-card p-3 shadow-sm ${isExploited ? 'border-danger' : ''}">
            <h6 class="fw-bold text-danger mb-1"><i class="bi bi-shield-exclamation"></i> ${title}</h6>
            <p class="small mb-2">${description}</p>
            <div class="d-flex gap-2">
                <span class="badge bg-dark">CWE-${cwe}</span>
                <span class="badge ${isExploited ? 'bg-danger' : 'bg-secondary'}">${isExploited ? '취약점 노출됨' : '보안 베스트 프랙티스'}</span>
            </div>
        </div>
    `;
    if (isExploited) {
        document.getElementById('pills-security-tab').classList.add('text-danger');
    } else {
        document.getElementById('pills-security-tab').classList.remove('text-danger');
    }
}

// 일반 흐름: 생성 -> 검증 -> 결제 수정
async function startPurchaseFlow(itemId, price) {
    clearModal();
    eduModal.show();

    try {
        // 1. RSA 키 자동 생성
        await callApi('POST', '/api/v1/users/1/keys');
        await callApi('POST', '/api/v1/users/2/keys');

        // 2. 거래 생성
        const tradeRes = await callApi('POST', '/api/v1/trades', {
            buyerId: 1,
            sellerId: 2,
            itemId: itemId,
            price: price
        });
        if (tradeRes.status !== 200) return showError('거래 생성에 실패했습니다.');
        currentTradeId = tradeRes.data.data.tradeId;

        // 3. 보안 검증 (해시/서명)
        const verifyRes = await callApi('PATCH', `/api/v1/trades/${currentTradeId}/verify`);
        if (verifyRes.status !== 200) return showError('보안 검증에 실패했습니다.');

        // 4. 결제 진행
        const payRes = await callApi('POST', '/api/v1/payments', {
            tradeId: currentTradeId,
            cardId: 1, // 테스트용 기본 카드
            amount: price
        });

        if (payRes.status !== 200) return showError('결제 처리 중 오류가 발생했습니다.');

        document.getElementById('userExperienceContent').innerHTML = `
            <div class="text-success"><i class="bi bi-check-circle-fill display-1"></i></div>
            <h4 class="fw-bold mt-3">거래 및 결제 완료!</h4>
            <p class="text-muted">보안 검증, 결제, 아이템 소유권 이전이 모두 성공적으로 처리되었습니다.</p>
            <div class="alert alert-success small mt-2">
                결제 번호: #${payRes.data.data.paymentId}<br>
                거래 상태: COMPLETED (완료)
            </div>
        `;

        setSecurityInsight(
            "전체 거래 프로세스 성공",
            "AES CBC 복호화, SHA-256 무결성 검증, RSA 전자서명 검증 및 결제 후 자동 소유권 이전이 완료되었습니다.",
            "해당 없음"
        );
    } catch (error) {
        showError('시스템 오류가 발생했습니다: ' + error.message);
    }
}

// 공격: 검증 건너뛰기
async function forcePayment() {
    clearModal();
    eduModal.show();

    // 1. 거래 생성
    document.getElementById('userExperienceContent').innerHTML = '<h6>[해커] 거래 생성 중...</h6>';
    const tradeRes = await callApi('POST', '/api/v1/trades', {
        buyerId: 1, sellerId: 2, itemId: 1, price: 50000 });
    currentTradeId = tradeRes.data.data.tradeId;

    // 2. 강제 결제 (검증 생략)
    document.getElementById('userExperienceContent').innerHTML = '<h6>[해커] 검증 없이 결제 시도 중...</h6>';
    const payRes = await callApi('POST', '/api/v1/payments', {
        tradeId: currentTradeId, cardId: 1, amount: 50000
    });

    if (payRes.status === 200) {
        document.getElementById('userExperienceContent').innerHTML = `
            <div class="text-danger"><i class="bi bi-exclamation-triangle-fill display-1"></i></div>
            <h4 class="fw-bold mt-3">공격 성공</h4>
            <p>검증되지 않은 아이템에 대해 결제가 완료되었습니다. 서버가 무결성 체크를 누락했습니다!</p>
        `;
        setSecurityInsight("비즈니스 로직 우회", "백엔드에서 결제 처리 전 거래 상태가 'VERIFIED'인지 확인하지 않았습니다. 공격자는 유효하지 않은 서명의 아이템도 강제로 구매할 수 있습니다.", "840", true);
    }
}

// 공격: IDOR 영수증 탈취
async function stealReceipt() {
    clearModal();
    eduModal.show();
    
    // 공격자 계정으로 자동 전환
    document.getElementById('userSelector').value = 'hacker';
    const targetId = currentPaymentId || 1;

    document.getElementById('userExperienceContent').innerHTML = `<h6>[해커] 영수증 #${targetId} 탈취 시도 중...</h6>`;
    const res = await callApi('GET', `/api/v1/payments/${targetId}`);

    if (res.status === 200) {
        document.getElementById('userExperienceContent').innerHTML = `
            <div class="text-danger"><i class="bi bi-shield-slash display-1"></i></div>
            <h4 class="fw-bold mt-3">데이터 유출 성공!</h4>
            <p>사용자 ID: 1 의 결제 상세 내역에 접근했습니다.</p>
            <div class="alert alert-secondary small">카드 정보: ${res.data.data.cardInfo.cardCompany} (${res.data.data.cardInfo.maskedCardNumber})</div>
        `;
        setSecurityInsight("IDOR (부적절한 직접 객체 참조)", "해커가 본인의 것이 아닌 결제 ID에 접근했습니다. 서버에서 요청자가 실제 구매자인지 검증하지 않았습니다.", "639", true);
    }
}

function showError(msg) {
    document.getElementById('userExperienceContent').innerHTML = `
        <div class="text-danger"><i class="bi bi-x-circle-fill display-1"></i></div>
        <h4 class="fw-bold mt-3">오류 발생</h4>
        <p>${msg}</p>
    `;
}

function changeRoleView() {
    const role = document.getElementById('roleSelector').value;

    const consumer = document.getElementById('consumerCardSection');
    const company = document.getElementById('cardCompanySection');
    const buyer = document.getElementById('buyerPaymentSection');
    const itemSection = document.getElementById('itemSection');

    if (consumer)
        consumer.style.display = role === 'consumer' ? 'block' : 'none';

    if (company)
        company.style.display = role === 'cardCompany' ? 'block' : 'none';

    if (buyer)
        buyer.style.display = role === 'buyer' ? 'block' : 'none';

    if (itemSection)
        itemSection.style.display = role === 'cardCompany' ? 'none' : 'block';
}