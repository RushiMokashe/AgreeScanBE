package com.myagree.app.farmer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myagree.app.farmer.dto.FarmerProfileResponse;

@RestController
@RequestMapping("/api/farmer")
class FarmerController {

    private final FarmerService farmerService;

    FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @GetMapping("/me")
    FarmerProfileResponse currentFarmer() {
        return farmerService.currentFarmer();
    }
}
