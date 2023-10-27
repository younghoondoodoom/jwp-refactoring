package kitchenpos.order.application;

import kitchenpos.menu.repository.MenuRepository;
import kitchenpos.order.application.dto.request.OrderCreateRequest;
import kitchenpos.order.application.dto.request.OrderLineItemRequest;
import kitchenpos.order.application.dto.request.OrderStatusChangeRequest;
import kitchenpos.order.application.dto.response.OrderResponse;
import kitchenpos.order.application.mapper.OrderMapper;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderLineItems;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderTableService orderTableService;

    public OrderService(
            final MenuRepository menuRepository,
            final OrderRepository orderRepository,
            final OrderTableService orderTableService) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.orderTableService = orderTableService;
    }

    @Transactional
    public OrderResponse create(final OrderCreateRequest orderCreateRequest) {
        checkOrderTableExists(orderCreateRequest);
        checkAllMenuExists(orderCreateRequest);
        final List<OrderLineItem> orderLineItems = makeOrderLineItems(orderCreateRequest);
        final Order order = new Order(orderCreateRequest.getOrderTableId(), new OrderLineItems(orderLineItems));
        final Order savedOrder = orderRepository.save(order);
        return OrderMapper.mapToResponse(savedOrder);
    }

    private void checkOrderTableExists(final OrderCreateRequest orderCreateRequest) {
        if (orderTableService.isOrderTableNotExist(orderCreateRequest.getOrderTableId())) {
            throw new IllegalArgumentException();
        }
    }

    private void checkAllMenuExists(final OrderCreateRequest orderCreateRequest) {
        final List<Long> menuIds = orderCreateRequest.getOrderLineItems()
                .stream()
                .map(OrderLineItemRequest::getMenuId)
                .collect(Collectors.toList());
        if (!menuRepository.existsAllByIdIn((menuIds))) {
            throw new IllegalArgumentException();
        }
    }

    private List<OrderLineItem> makeOrderLineItems(final OrderCreateRequest orderCreateRequest) {
        return orderCreateRequest.getOrderLineItems()
                .stream()
                .map(it -> new OrderLineItem(it.getMenuId(), it.getQuantity()))
                .collect(Collectors.toList());
    }

    public List<OrderResponse> list() {
        final List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(OrderMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse changeOrderStatus(final Long orderId, final OrderStatusChangeRequest orderStatusChangeRequest) {
        final OrderStatus orderStatus = OrderMapper.mapToOrderStatus(orderStatusChangeRequest);
        final Order savedOrder = orderRepository.findById(orderId)
                .orElseThrow(IllegalArgumentException::new);
        savedOrder.changeOrderStatus(orderStatus);
        return OrderMapper.mapToResponse(orderRepository.save(savedOrder));
    }
}
