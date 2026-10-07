package com.myagree.app.store.dto;

import java.util.List;

/**
 * The shop portal's home ({@code GET /api/shop/dashboard}); mirrors {@code ShopDashboard} in frontend/src/lib/types.ts.
 *
 * @param ordersToday       orders placed with the shop since midnight (India time)
 * @param salesToday        their total in rupees
 * @param paymentsToVerify  orders paid by Scan & Pay that wait for the shopkeeper to confirm the money arrived
 * @param acceptsScanAndPay whether the shop has set up Scan & Pay
 * @param recentOrders      the latest orders, newest first
 */
public record ShopDashboardResponse(
        String shopName,
        int ordersToday,
        long salesToday,
        int paymentsToVerify,
        int products,
        int outOfStock,
        boolean acceptsScanAndPay,
        List<ShopOrderResponse> recentOrders) {
}
