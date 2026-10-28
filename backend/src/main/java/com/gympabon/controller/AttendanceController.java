package com.gympabon.controller;

import com.gympabon.controller.dto.AttendanceResponse;
import com.gympabon.controller.dto.EntryRequest;
import com.gympabon.model.Attendance;
import com.gympabon.model.User;
import com.gympabon.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrance endpoints: registering entries and looking at the history.
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SessionManager sessionManager;

    public AttendanceController(AttendanceService attendanceService, SessionManager sessionManager) {
        this.attendanceService = attendanceService;
        this.sessionManager = sessionManager;
    }

    /**
     * POST /api/attendance: registers an entry attempt at this moment. The
     * answer says whether it was allowed and, if not, why.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse registerEntry(@RequestBody EntryRequest request, HttpSession session) {
        User user = sessionManager.requireUser(session);
        return AttendanceResponse.from(
                attendanceService.registerEntry(request.documentNumber(), LocalDateTime.now(), user));
    }

    /**
     * GET /api/attendance?date=2026-10-27: entries of one day (today if empty).
     * GET /api/attendance?from=2026-10-01&to=2026-10-27: entries of a period.
     */
    @GetMapping
    public List<AttendanceResponse> list(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            HttpSession session) {
        sessionManager.requireUser(session);
        List<Attendance> records = from != null || to != null
                ? attendanceService.listByDateRange(from, to)
                : attendanceService.listByDate(date);
        return records.stream().map(AttendanceResponse::from).toList();
    }

    /** GET /api/attendance/client/{clientId}: entry history of a client. */
    @GetMapping("/client/{clientId}")
    public List<AttendanceResponse> listByClient(@PathVariable("clientId") int clientId, HttpSession session) {
        sessionManager.requireUser(session);
        return attendanceService.listByClient(clientId).stream().map(AttendanceResponse::from).toList();
    }
}
