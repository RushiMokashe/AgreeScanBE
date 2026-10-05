package com.myagree.app.treatment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.treatment.dto.TreatmentPlanResponse;

@RestController
@RequestMapping("/api/treatment-plans")
class TreatmentController {

    private final TreatmentService treatmentService;

    TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping("/active")
    TreatmentPlanResponse activePlan() {
        return treatmentService.activePlan();
    }

    @GetMapping("/{planId}")
    TreatmentPlanResponse plan(@PathVariable long planId) {
        return treatmentService.getPlan(planId);
    }

    @PostMapping("/{planId}/steps/{stepId}/complete")
    TreatmentPlanResponse completeStep(@PathVariable long planId, @PathVariable long stepId) {
        return treatmentService.completeStep(planId, stepId);
    }

    @PostMapping("/{planId}/steps/{stepId}/reminder")
    TreatmentPlanResponse toggleStepReminder(@PathVariable long planId, @PathVariable long stepId) {
        return treatmentService.toggleStepReminder(planId, stepId);
    }
}
