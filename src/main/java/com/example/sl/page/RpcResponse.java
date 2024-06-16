package com.example.sl.page;

import lombok.Data;

@Data
 class RpcResponse<T> {
 private Boolean success;
 private T data;
 // 分⻚令牌，如果返回值不为空，则通过该令牌能够执⾏下⼀次查询
 private byte[] token;

 public RpcResponse(Boolean success, T data, byte[] token) {
  this.success = success;
  this.data = data;
  this.token = token;
 }
}

