package com.thinkerwolf.gamer.rpc.protocol.tcp;

import com.thinkerwolf.gamer.common.concurrent.Promise;
import com.thinkerwolf.gamer.rpc.*;
import com.thinkerwolf.gamer.rpc.protocol.AbstractInvoker;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wukai
 * @date 2020/5/14 10:01
 */
public class TcpInvoker<T> extends AbstractInvoker<T> {

    private final AtomicInteger round = new AtomicInteger();

    private ExchangeClient[] clients;

    public TcpInvoker(ExchangeClient[] clients) {
        this.clients = clients;
    }

    @Override
    protected ExchangeClient<RpcResponse> nextClient() {
        return clients[nextIdx(round, clients.length)];
    }

    @Override
    public boolean isUsable() {
        return true;
    }
}
