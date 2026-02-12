package com.example.order_service.entity;
import jakarta.persistence.*;


@Entity
@Table(name = "orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long userId;
  private String product;
  private int qty;

  @Enumerated(EnumType.STRING)
  private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;


  // getters & setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getProduct() { return product; }
  public void setProduct(String product) { this.product = product; }
  public int getQty() { return qty; }
  public void setQty(int qty) { this.qty = qty; }
  public OrderStatus getStatus() {
        return status;
    }
  public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public SagaStatus getSagaStatus() {
        return sagaStatus;
    }
    public void setSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }
}
