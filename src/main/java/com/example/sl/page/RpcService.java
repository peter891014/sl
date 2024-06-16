package com.example.sl.page;

import java.util.List;

public interface RpcService {
    public <T> RpcResponse<List<T>> listMetrics(BaseMetericQuery query);
}
