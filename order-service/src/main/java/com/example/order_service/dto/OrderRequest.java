package com.example.order_service.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderRequest {

  @NotNull private Long userId;
  @NotNull private String product;
  @Min(1) private int qty;

  // getters & setters
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getProduct() { return product; }
  public void setProduct(String product) { this.product = product; }
  public int getQty() { return qty; }
  public void setQty(int qty) { this.qty = qty; }
}
