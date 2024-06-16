package com.example.sl.page;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MetricQueryPager extends BaseMetericQuery {


    public static <T, Q extends BaseMetericQuery> List<T> autoPaged(Function<Q, RpcResponse<List<T>>> queryFunction, Q initialQuery) {
        List<T> result = new ArrayList<>();
        Q currentQuery = initialQuery;
        boolean hasMore = true;
        while (hasMore) {
            RpcResponse<List<T>> response = queryFunction.apply(currentQuery);
            result.addAll(response.getData());
            hasMore = response.getToken() != null;
            if (hasMore) {
                currentQuery.setToken(response.getToken());
            }
        }
        return result;
    }

    public static void main(String[] args) {
        RpcService rpcClient = new RpcServiceImpl();
        List<MetricXxxDTO> xxx = MetricQueryPager.autoPaged(
                rpcClient::listMetrics,
                new MetricXxxQuery());
        System.out.println(xxx.size());
    }

}