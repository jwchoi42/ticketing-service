package dev.ticketing.core.payment.adapter.in.web;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.ticketing.TicketingService;
import dev.ticketing.core.payment.adapter.in.web.model.ConfirmPaymentRequest;
import dev.ticketing.core.payment.adapter.in.web.model.RequestPaymentRequest;
import dev.ticketing.core.payment.adapter.in.web.model.response.PaymentResponse;
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
    private final TicketingService ticketingOrchestrationService;

    @Operation(summary = "결제 요청")
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<PaymentResponse> requestPayment(@RequestBody final RequestPaymentRequest request) {
        final Payment payment = requestPaymentUseCase.requestPayment(request.toCommand());
        return SuccessResponse.of(PaymentResponse.from(payment));
    }

    @Operation(summary = "결제 승인 (최종 확정)")
    @PostMapping("/confirm")
    public SuccessResponse<PaymentResponse> confirmPayment(@RequestBody final ConfirmPaymentRequest request) {
        final Payment payment = ticketingOrchestrationService.confirmPaymentAndFinalizeReservation(request.toCommand());
        return SuccessResponse.of(PaymentResponse.from(payment));
    }
}
