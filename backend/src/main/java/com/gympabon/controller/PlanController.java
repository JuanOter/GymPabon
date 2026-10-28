package com.gympabon.controller;

import com.gympabon.controller.dto.PlanResponse;
import com.gympabon.controller.dto.PlanUpdateRequest;
import com.gympabon.service.PlanService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plan endpoints. Anyone signed in can see the plans; only the administrator
 * can change their price and description.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;
    private final SessionManager sessionManager;

    public PlanController(PlanService planService, SessionManager sessionManager) {
        this.planService = planService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/plans */
    @GetMapping
    public List<PlanResponse> list(HttpSession session) {
        sessionManager.requireUser(session);
        return planService.listPlans().stream().map(PlanResponse::from).toList();
    }

    /** GET /api/plans/{id} */
    @GetMapping("/{id}")
    public PlanResponse findById(@PathVariable("id") int id, HttpSession session) {
        sessionManager.requireUser(session);
        return PlanResponse.from(planService.findById(id));
    }

    /** PUT /api/plans/{id} (administrator): new monthly price and description. */
    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable("id") int id, @RequestBody PlanUpdateRequest request,
                               HttpSession session) {
        sessionManager.requireAdmin(session);
        planService.updatePlan(id, request.monthlyPrice(), request.description());
        return PlanResponse.from(planService.findById(id));
    }
}
