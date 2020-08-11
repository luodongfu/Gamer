package com.thinkerwolf.gamer.grizzly;


import com.thinkerwolf.gamer.common.URL;
import com.thinkerwolf.gamer.grizzly.tcp.PacketFilter;
import com.thinkerwolf.gamer.grizzly.websocket.DefaultApplication;
import com.thinkerwolf.gamer.grizzly.websocket.WebSocketClientFilter;
import com.thinkerwolf.gamer.grizzly.websocket.WebSocketServerFilter;
import com.thinkerwolf.gamer.remoting.ChannelHandler;
import com.thinkerwolf.gamer.remoting.Protocol;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import org.glassfish.grizzly.websockets.WebSocketEngine;


public final class FilterChains {

    public static Processor createProcessor(boolean server, URL url, ChannelHandler handler) {
        Protocol protocol = Protocol.parseOf(url.getProtocol());
        FilterChainBuilder builder = FilterChainBuilder.stateless();
        if (protocol.equals(Protocol.TCP)) {
            builder.addLast(new TransportFilter());
            builder.addLast(new PacketFilter());
            builder.addLast(server ? new GrizzlyServerFilter(url, handler) : new GrizzlyClientFilter(url, handler));
        } else if (protocol.equals(Protocol.HTTP)
                || protocol.equals(Protocol.WEBSOCKET)) {
            if (server) {
                WebSocketEngine.getEngine().register("", "/*", new DefaultApplication(url, handler));
            }
            final DelayedExecutor timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor();
            timeoutExecutor.start();
            builder.addLast(new TransportFilter());
            builder.addLast(server ? new HttpServerFilter() : new HttpClientFilter());
            if (server || protocol.equals(Protocol.WEBSOCKET)) {
                builder.addLast(server ? new WebSocketServerFilter() : new WebSocketClientFilter());
            }
            builder.addLast(server ? new GrizzlyServerFilter(url, handler) : new GrizzlyClientFilter(url, handler));
        }
        return builder.build();
    }


}
