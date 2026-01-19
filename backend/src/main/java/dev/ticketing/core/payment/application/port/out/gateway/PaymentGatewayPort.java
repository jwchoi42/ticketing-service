package dev.ticketing.core.payment.application.port.out.gateway;

public interface PaymentGatewayPort {
    boolean executePayment(String paymentKey, String orderId, Integer amount);
}
