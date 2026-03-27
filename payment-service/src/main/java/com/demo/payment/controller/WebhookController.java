package com.demo.payment.controller;

import com.demo.payment.dto.PaymentWebhookDTO;
import com.demo.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/payment")
@Tag(name = "Payment Webhooks", description = "Simulated PSP webhook callbacks")
public class WebhookController {

    private final PaymentService paymentService;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Receive payment webhook",
               description = "Simulates a PSP callback to complete or fail a pending payment")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or payment not in PENDING state")
    @ApiResponse(responseCode = "404", description = "Payment not found for this order")
    @PostMapping
    public ResponseEntity<String> handleWebhook(@Valid @RequestBody PaymentWebhookDTO dto) {
        log.info("Webhook received: orderId={}, status={}", dto.orderId(), dto.status());

         switch (dto.status().toUpperCase()) {
            case "COMPLETED" -> {
                paymentService.completeFromWebhook(dto.orderId());
                return ResponseEntity.ok("Payment completed for orderId=" + dto.orderId());
            }
            case "FAILED" -> {
                String reason = dto.reason() != null ? dto.reason() : "Payment declined by PSP";
                paymentService.failFromWebhook(dto.orderId(), reason);
                return ResponseEntity.ok("Payment failed for orderId=" + dto.orderId());
            }
             default -> {
                 return ResponseEntity.badRequest()
                         .body("Invalid status: " + dto.status() + ". Expected COMPLETED or FAILED.");
             }
         }
    }
}
