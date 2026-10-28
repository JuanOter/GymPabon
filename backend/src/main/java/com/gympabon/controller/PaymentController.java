package com.gympabon.controller;

import com.gympabon.controller.dto.PaymentRequest;
import com.gympabon.controller.dto.PaymentResponse;
import com.gympabon.controller.dto.PendingMembershipResponse;
import com.gympabon.controller.dto.VoidRequest;
import com.gympabon.model.Payment;
import com.gympabon.model.PaymentMethod;
import com.gympabon.model.User;
import com.gympabon.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoints: collecting, listing and voiding payments.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final SessionManager sessionManager;

    public PaymentController(PaymentService paymentService, SessionManager sessionManager) {
        this.paymentService = paymentService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/payments/pending: memberships waiting for their payment. */
    @GetMapping("/pending")
    public List<PendingMembershipResponse> pending(HttpSession session) {
        sessionManager.requireUser(session);
        return paymentService.listPendingMemberships().stream().map(PendingMembershipResponse::from).toList();
    }

    /** GET /api/payments?from=2026-10-01&to=2026-10-31&method=CASH (method is optional). */
    @GetMapping
    public List<PaymentResponse> list(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "method", required = false) PaymentMethod method, HttpSession session) {
        sessionManager.requireUser(session);
        return paymentService.listPayments(from, to, method).stream().map(PaymentResponse::from).toList();
    }

    /** POST /api/payments: registers the payment; the signed-in user is who received it. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse register(@RequestBody PaymentRequest request, HttpSession session) {
        User user = sessionManager.requireUser(session);
        Payment payment = paymentService.registerPayment(request.membershipId(), request.method(), user);
        return PaymentResponse.from(payment);
    }

    /** PATCH /api/payments/{id}/void (administrator) */
    @PatchMapping("/{id}/void")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void voidPayment(@PathVariable("id") int id, @RequestBody VoidRequest request, HttpSession session) {
        User admin = sessionManager.requireAdmin(session);
        paymentService.voidPayment(id, request.reason(), admin);
    }
}
