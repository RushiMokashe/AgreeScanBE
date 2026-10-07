package com.myagree.app.care;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.care.dto.AgronomistResponse;
import com.myagree.app.care.dto.NearbyStockResponse;
import com.myagree.app.common.i18n.Language;

@RestController
@RequestMapping("/api/care")
class CareController {

    private final CareService careService;

    CareController(CareService careService) {
        this.careService = careService;
    }

    @GetMapping("/nearby-stock")
    NearbyStockResponse nearbyStock(Language language) {
        return careService.nearbyStock(language);
    }

    @GetMapping("/agronomist")
    AgronomistResponse agronomist() {
        return careService.agronomistOnDuty();
    }
}
