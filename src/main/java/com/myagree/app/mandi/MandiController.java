package com.myagree.app.mandi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.mandi.dto.BuyerInquiryResponse;
import com.myagree.app.mandi.dto.MandiOverviewResponse;

@RestController
@RequestMapping("/api/mandi")
class MandiController {

    private final MandiService mandiService;

    MandiController(MandiService mandiService) {
        this.mandiService = mandiService;
    }

    @GetMapping
    MandiOverviewResponse overview() {
        return mandiService.overview();
    }

    @PostMapping("/inquiries/{id}/respond")
    BuyerInquiryResponse respondToInquiry(@PathVariable long id) {
        return mandiService.respondToInquiry(id);
    }
}
