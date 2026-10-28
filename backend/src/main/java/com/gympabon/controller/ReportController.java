package com.gympabon.controller;

import com.gympabon.controller.dto.ExpiringMembershipResponse;
import com.gympabon.service.ReportService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Figures for the main panel (any signed-in user) and reports (administrator).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final SessionManager sessionManager;

    public ReportController(ReportService reportService, SessionManager sessionManager) {
        this.reportService = reportService;
        this.sessionManager = sessionManager;
    }

    /** GET /api/reports/dashboard: figures of the main panel for today. */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpSession session) {
        sessionManager.requireUser(session);
        return reportService.getDashboardSummary(LocalDate.now());
    }

    /** GET /api/reports/expiring?days=7: active memberships that end soon. */
    @GetMapping("/expiring")
    public List<ExpiringMembershipResponse> expiring(@RequestParam(name = "days", defaultValue = "7") int days,
                                                     HttpSession session) {
        sessionManager.requireUser(session);
        return reportService.getExpiringMemberships(days).stream().map(ExpiringMembershipResponse::from).toList();
    }

    /** GET /api/reports/income-by-plan?from=2026-10-01&to=2026-10-31 (administrator) */
    @GetMapping("/income-by-plan")
    public Map<String, Double> incomeByPlan(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, HttpSession session) {
        sessionManager.requireAdmin(session);
        return reportService.getIncomeByPlan(from, to);
    }

    /** GET /api/reports/clients-by-plan (administrator) */
    @GetMapping("/clients-by-plan")
    public Map<String, Integer> clientsByPlan(HttpSession session) {
        sessionManager.requireAdmin(session);
        return reportService.getClientsByPlan();
    }

    /** GET /api/reports/attendance-by-day?from=2026-10-01&to=2026-10-31 (administrator) */
    @GetMapping("/attendance-by-day")
    public Map<LocalDate, Integer> attendanceByDay(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, HttpSession session) {
        sessionManager.requireAdmin(session);
        return reportService.getAttendanceByDay(from, to);
    }
}
