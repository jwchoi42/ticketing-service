package dev.ticketing.core.payment.adapter.in.web;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.payment.adapter.in.web.model.ConfirmPaymentRequest;
import dev.ticketing.core.payment.adapter.in.web.model.RequestPaymentRequest;
import dev.ticketing.core.payment.application.port.in.ConfirmPaymentUseCase;
import dev.ticketing.core.payment.application.port.in.RequestPaymentUseCase;
import dev.ticketing.core.payment.domain.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final RequestPaymentUseCase requestPaymentUseCase;
    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    @Operation(summary = "결제 요청")
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<Payment> requestPayment(@RequestBody RequestPaymentRequest request) {
        Payment payment = requestPaymentUseCase.requestPayment(request.toCommand());
        return SuccessResponse.of(payment);
    }

    @Operation(summary = "결제 승인 (최종 확정)")
    @PostMapping("/confirm")
    public SuccessResponse<Payment> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        Payment payment = confirmPaymentUseCase.confirmPayment(request.toCommand());
        return SuccessResponse.of(payment);
    }
}
