package com.myagree.app.dashboard.dto;

import com.myagree.app.dashboard.AlertSeverity;

/** Mirrors {@code RiskAlert} in frontend/src/lib/types.ts. */
public record RiskAlertResponse(long id, String title, String message, AlertSeverity severity, String region) {
}
