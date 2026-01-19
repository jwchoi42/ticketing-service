package dev.ticketing.core.payment.adapter.out.gateway;

import dev.ticketing.core.payment.application.port.out.gateway.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class TossPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public boolean executePayment(String paymentKey, String orderId, Integer amount) {
        log.info("[TossPG] 결제 승인 요청: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        // Toss Payments 시뮬레이션
        // 특정 키워드가 포함된 경우 실패 처리 (테스트 용도)
        if (paymentKey != null && paymentKey.contains("fail")) {
            log.warn("[TossPG] 결제 승인 거절 (Simulated Failure): 잔액 부족 등의 사유");
            return false;
        }

        // 입력값 검증 (Null Check)
        if (paymentKey == null || orderId == null || amount == null) {
            log.error("[TossPG] 결제 승인 실패: 필수 값 누락");
            return false;
        }

        // 성공 처리
        log.info("[TossPG] 결제 승인 완료: transactionId={}", UUID.randomUUID());
        return true;
    }
}
