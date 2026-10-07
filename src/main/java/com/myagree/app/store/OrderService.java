package com.myagree.app.store;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.store.dto.OrderSummaryResponse;

/** The farmer's orders; another farmer's order answers 404. */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final String ORDER_NOT_FOUND = "store.order.not-found";

    private final OrderRepository orderRepository;
    private final StoreMapper mapper;

    OrderService(OrderRepository orderRepository, StoreMapper mapper) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    /** The farmer's orders with their lines, newest first. */
    public List<OrderSummaryResponse> orders(long farmerId, Language language) {
        return orderRepository.findByFarmerIdOrderByPlacedAtDescIdDesc(farmerId).stream()
                .map(order -> mapper.toSummary(order, language))
                .toList();
    }

    /**
     * One of the farmer's orders.
     *
     * @throws NotFoundException when the farmer has no such order
     */
    public OrderSummaryResponse order(long farmerId, long orderId, Language language) {
        return orderRepository.findWithLinesByIdAndFarmerId(orderId, farmerId)
                .map(order -> mapper.toSummary(order, language))
                .orElseThrow(() -> new NotFoundException(UserMessage.of(ORDER_NOT_FOUND, String.valueOf(orderId))));
    }
}
