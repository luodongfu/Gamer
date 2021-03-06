package com.thinkerwolf.gamer.remoting;

import com.thinkerwolf.gamer.common.concurrent.Promise;

import java.net.SocketAddress;

/**
 * Channel
 *
 * @author wukai
 */
public interface Channel extends Endpoint {

    Object id();

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();

    Object innerCh();

    Object getAttr(String key);

    void setAttr(String key, Object value);

    Promise<Channel> sendPromise(Object message);
    /**
     * 是否再连接中
     *
     * @return
     */
    boolean isConnected();

}
