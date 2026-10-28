package com.gympabon.controller;

import com.gympabon.controller.dto.CancelRequest;
import com.gympabon.controller.dto.DurationResponse;
import com.gympabon.controller.dto.MembershipRequest;
import com.gympabon.controller.dto.MembershipResponse;
import com.gympabon.controller.dto.RenewalRequest;
import com.gympabon.controller.dto.TrainerChangeRequest;
import com.gympabon.model.Membership;
import com.gympabon.model.MembershipDuration;
import com.gympabon.service.MembershipService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * Membership endpoints: sale, renewal, quote and cancellation.
 */
@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;
    private final SessionManager sessionManager;

    public MembershipController(MembershipService membershipService, SessionManager sessionManager) {
        this.membershipService = membershipService;
        this.sessionManager = sessionManager;
    }

    /**
     * GET /api/memberships?clientId=3: membership history of a client. Without
     * clientId it returns every membership (used by the lists of the screens).
     */
    @GetMapping
    public List<MembershipResponse> findByClient(@RequestParam(name = "clientId", required = false) Integer clientId,
                                                 HttpSession session) {
        sessionManager.requireUser(session);
        List<Membership> memberships = clientId == null
                ? membershipService.listAll()
                : membershipService.findByClient(clientId);
        return memberships.stream().map(MembershipResponse::from).toList();
    }

    /** GET /api/memberships/{id} */
    @GetMapping("/{id}")
    public MembershipResponse findById(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireUser(session);
        return MembershipResponse.from(membershipService.findById(id));
    }

    /** GET /api/memberships/durations: the durations that can be sold, with their discount. */
    @GetMapping("/durations")
    public List<DurationResponse> durations(HttpSession session) {
        sessionManager.requireUser(session);
        return Arrays.stream(MembershipDuration.values()).map(DurationResponse::from).toList();
    }

    /** GET /api/memberships/quote?clientId=1&planId=2&duration=THREE_MONTHS: value before selling. */
    @GetMapping("/quote")
    public Map<String, Double> quote(@RequestParam("clientId") int clientId, @RequestParam("planId") int planId,
                                     @RequestParam("duration") MembershipDuration duration, HttpSession session) {
        sessionManager.requireUser(session);
        return Map.of("totalAmount", membershipService.quote(clientId, planId, duration));
    }

    /** POST /api/memberships: sells a new membership, pending payment. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@RequestBody MembershipRequest request, HttpSession session) {
        sessionManager.requireUser(session);
        return MembershipResponse.from(membershipService.createMembership(request.clientId(), request.planId(),
                request.duration(), request.startDate(), request.trainerId()));
    }

    /** POST /api/memberships/renew: renews the active membership of a client. */
    @PostMapping("/renew")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse renew(@RequestBody RenewalRequest request, HttpSession session) {
        sessionManager.requireUser(session);
        return MembershipResponse.from(membershipService.renewMembership(request.clientId(), request.planId(),
                request.duration(), request.trainerId()));
    }

    /** PATCH /api/memberships/{id}/cancel (administrator) */
    @PatchMapping("/{id}/cancel")
    public MembershipResponse cancel(@PathVariable("id") int id, @RequestBody CancelRequest request,
                                     HttpSession session) {
        sessionManager.requireAdmin(session);
        membershipService.cancelMembership(id, request.reason());
        return MembershipResponse.from(membershipService.findById(id));
    }

    /** PATCH /api/memberships/{id}/trainer (administrator): assigns another trainer to a Premium membership. */
    @PatchMapping("/{id}/trainer")
    public MembershipResponse changeTrainer(@PathVariable("id") int id, @RequestBody TrainerChangeRequest request,
                                            HttpSession session) {
        sessionManager.requireAdmin(session);
        return MembershipResponse.from(membershipService.changeTrainer(id, request.trainerId()));
    }

    /** POST /api/memberships/refresh (administrator): updates the statuses for today. */
    @PostMapping("/refresh")
    public Map<String, Integer> refreshStatuses(HttpSession session) {
        sessionManager.requireAdmin(session);
        return Map.of("changed", membershipService.refreshStatuses(LocalDate.now()));
    }
}
