package com.myagree.app.store;

import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.store.dto.ShopDashboardResponse;
import com.myagree.app.store.dto.ShopOrderResponse;
import com.myagree.app.store.dto.ShopProfileRequest;
import com.myagree.app.store.dto.ShopProfileResponse;

/** The shopkeeper portal: the shop's home screen, its details and Scan & Pay set-up, and its orders. */
@RestController
@RequestMapping("/api/shop")
class ShopController {

    private final ShopService shopService;

    ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/dashboard")
    ShopDashboardResponse dashboard(CurrentUser user, Language language) {
        return shopService.dashboard(user.requireShopId(), language);
    }

    @GetMapping("/me")
    ShopProfileResponse profile(CurrentUser user) {
        return shopService.profile(user.requireShopId());
    }

    @PutMapping("/me")
    ShopProfileResponse updateProfile(CurrentUser user, @Valid @RequestBody ShopProfileRequest request) {
        return shopService.updateProfile(user.requireShopId(), request);
    }

    /** The shop's own UPI QR (a photo or screenshot from its payments app), shown to farmers who pay by Scan & Pay. */
    @PostMapping(path = "/me/upi-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ShopProfileResponse uploadUpiQr(CurrentUser user, @RequestPart("image") MultipartFile image) {
        return shopService.uploadUpiQr(user.requireShopId(), image);
    }

    @DeleteMapping("/me/upi-qr")
    ShopProfileResponse removeUpiQr(CurrentUser user) {
        return shopService.removeUpiQr(user.requireShopId());
    }

    /** Newest first; {@code status=VERIFYING_PAYMENT} lists the Scan & Pay payments to confirm. */
    @GetMapping("/orders")
    List<ShopOrderResponse> orders(CurrentUser user, @RequestParam(required = false) @Nullable OrderStatus status,
                                   Language language) {
        return shopService.orders(user.requireShopId(), status, language);
    }
}
