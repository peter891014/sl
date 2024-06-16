package com.example.sl.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class RpcServiceImpl implements RpcService {
    @Override
    public <T> RpcResponse<List<T>> listMetrics(BaseMetericQuery query) {
        return new RpcResponse<>(true, mockDataList(query.getClass()), mockToken(query.getToken()));
    }

    private <T> List<T> mockDataList(Class<? extends BaseMetericQuery> queryType) {

        return new ArrayList<>(200);
    }

    private byte[] mockToken(byte[] currentToken) {
        Random random = new Random();
        int n= random.nextInt(100);
        System.out.println(n);
        return n > 90 ? null : "abc".getBytes();
    }
}
