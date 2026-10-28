package com.gympabon.controller.dto;

import com.gympabon.model.Attendance;
import java.time.LocalDateTime;

/**
 * An entry attempt as the frontend sees it, allowed or rejected with its reason.
 */
public record AttendanceResponse(int id, int clientId, String clientDocument, String clientName,
                                 LocalDateTime dateTime, boolean allowed, String rejectionReason,
                                 String registeredBy) {

    /**
     * Builds the response from a domain record.
     *
     * @param attendance domain record with its client and user
     * @return the response
     */
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(attendance.getId(), attendance.getClient().getId(),
                attendance.getClient().getDocumentNumber(), attendance.getClient().getFullName(),
                attendance.getDateTime(), attendance.isAllowed(), attendance.getRejectionReason(),
                attendance.getRegisteredBy().getUsername());
    }
}
