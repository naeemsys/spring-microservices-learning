package com.example.order_service.controller;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.service.OrderSagaService;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  private final OrderService svc;
  private final OrderSagaService orderSagaService;
  public OrderController(OrderService svc, OrderSagaService orderSagaService) { this.svc = svc;
      this.orderSagaService = orderSagaService;
  }

  @PostMapping
  public ResponseEntity<Order> create(@Valid @RequestBody OrderRequest req){
    Order created = svc.create(req);
    return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
  }

  @PostMapping("/saga")
  public Order createOrderBySaga(@RequestBody OrderRequest req) {
      return orderSagaService.startSaga(req);
  }

  @GetMapping
  public ResponseEntity<List<Order>> list(){ return ResponseEntity.ok(svc.list()); }
}
