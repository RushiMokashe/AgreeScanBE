package com.myagree.app.care;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.care.dto.AgronomistResponse;
import com.myagree.app.care.dto.NearbyStockResponse;

@RestController
@RequestMapping("/api/care")
class CareController {

    private final CareService careService;

    CareController(CareService careService) {
        this.careService = careService;
    }

    @GetMapping("/nearby-stock")
    NearbyStockResponse nearbyStock() {
        return careService.nearbyStock();
    }

    @GetMapping("/agronomist")
    AgronomistResponse agronomist() {
        return careService.agronomistOnDuty();
    }
}
