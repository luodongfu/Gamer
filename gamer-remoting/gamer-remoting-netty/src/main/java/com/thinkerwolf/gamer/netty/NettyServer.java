package com.thinkerwolf.gamer.netty;

import com.thinkerwolf.gamer.common.URL;
import com.thinkerwolf.gamer.common.log.InternalLoggerFactory;
import com.thinkerwolf.gamer.common.log.Logger;
import com.thinkerwolf.gamer.remoting.ChannelHandler;
import com.thinkerwolf.gamer.remoting.RemotingException;
import com.thinkerwolf.gamer.remoting.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.collections.MapUtils;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

public class NettyServer implements Server {

    private static final Logger LOG = InternalLoggerFactory.getLogger(NettyServer.class);

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    private final URL url;

    private final ChannelHandler handler;
    /**
     * 第二个Handler，目前用于从Http升级到Websocket时的处理器
     */
    private final ChannelHandler secondHandler;

    private Channel channel;

    private volatile boolean started;

    public NettyServer(URL url, ChannelHandler handler) {
        this(url, handler, null);
    }

    public NettyServer(URL url, ChannelHandler handler, ChannelHandler secondHandler) {
        this.url = url;
        this.handler = handler;
        this.secondHandler = secondHandler;
    }

    public synchronized void startup() throws Exception {
        if (started) {
            return;
        }
        started = true;
        String bossName = "NettyBoss_" + url.getProtocol();
        String workerName = "NettyWorker_" + url.getProtocol();
        int workerThreads = MapUtils.getInteger(url.getParameters(), URL.WORKER_THREADS);

        this.serverBootstrap = new ServerBootstrap();
        ServerBootstrap sb = this.serverBootstrap;
        this.bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(bossName));
        this.workerGroup = new NioEventLoopGroup(workerThreads, new DefaultThreadFactory(workerName));
        sb.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

        Map<String, Object> options = url.getObject(URL.OPTIONS, Collections.emptyMap());
        for (Map.Entry<String, Object> op : options.entrySet()) {
            if (ChannelOption.exists(op.getKey())) {
                sb.option(ChannelOption.valueOf(op.getKey()), op.getValue());
            }
        }

        Map<String, Object> childOptions = url.getObject(URL.CHILD_OPTIONS, Collections.emptyMap());
        for (Map.Entry<String, Object> op : childOptions.entrySet()) {
            if (ChannelOption.exists(op.getKey())) {
                sb.childOption(ChannelOption.valueOf(op.getKey().toUpperCase()), op.getValue());
            }
        }

        sb.handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(ChannelHandlers.createChannelInitializer0(url, handler, secondHandler));
        ChannelFuture future = sb.bind(new InetSocketAddress(url.getPort()));
        this.channel = future.channel();
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                LOG.info("Listen @" + url.getProtocol() + " on @" + url.getPort() + " success");
            } else {
                LOG.error("Can't start server", f.cause());
            }
        });
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        // TODO
    }

    @Override
    public void send(Object message) throws RemotingException {
        // TODO
    }

    @Override
    public void close() {
        NettyChannel nc = NettyChannel.getOrAddChannel(channel, url, handler);
        if (nc != null && !nc.isClosed()) {
            nc.close();

        }
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public boolean isClosed() {
        NettyChannel nc = NettyChannel.getOrAddChannel(channel, url, handler);
        return nc == null || nc.isClosed();
    }
}
