/*
 * Copyright 2014 Alexey Plotnik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stem.client.v2;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stem.exceptions.ClientTransportException;
import org.stem.exceptions.ConnectionException;
import org.stem.transport.*;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

public class Connection
{
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private InetSocketAddress address;
    private Factory factory;
    protected Channel channel;
    private final Dispatcher dispatcher = new Dispatcher();
    private volatile boolean isReady;
    private volatile boolean isDisconnecting;
    private final AtomicReference<ConnectionCloseFuture> closeFutureRef = new AtomicReference<>();

    public Connection(InetSocketAddress address, Factory factory) throws ConnectionException
    {
        this.address = address;
        this.factory = factory;

        Bootstrap bootstrap = factory.createNewBootstrap();
        bootstrap
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelHandler(this));

        ChannelFuture future = bootstrap.connect(address);
        this.channel = future.awaitUninterruptibly().channel();
        if (!future.isSuccess())
        {
            throw disconnecting(new ClientTransportException(address, "Can't connect", future.cause()));
        }
        isReady = true;
    }

    private <E extends Exception> E disconnecting(E e)
    {
        isDisconnecting = true;

        close().force();

        return e;
    }

    public Future write(Message.Request request)
    {
        Future future = new Future(request);
        write(future);
        return future;
    }

    private ResponseHandler write(ResponseCallback callback)
    {
        Message.Request request = callback.request();
        ResponseHandler responseHandler = new ResponseHandler(this, callback);
        dispatcher.addHandler(responseHandler);

        return null;
    }

    public CloseFuture close()
    {
        ConnectionCloseFuture future = new ConnectionCloseFuture();
        if (!closeFutureRef.compareAndSet(null, future))
        {
            return closeFutureRef.get();
        }

        logger.debug("Closing connection");

        //if (dispatcher.pending.isEmpty())
        //    future.force();
        return future;
    }

    private class ConnectionCloseFuture extends CloseFuture
    {

        @Override
        public ConnectionCloseFuture force()
        {
            return null;
        }
    }

    public static class ChannelHandler extends ChannelInitializer<SocketChannel>
    {

        private static final PacketDecoder packetDecoder = new PacketDecoder();
        private static final PacketEncoder packetEncoder = new PacketEncoder();
        private static final MessageDecoder messageDecoder = new MessageDecoder();
        private static final MessageEncoder messageEncoder = new MessageEncoder();
        private Connection connection;

        public ChannelHandler(Connection connection)
        {
            this.connection = connection;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception
        {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("packetDecoder", packetDecoder);
            pipeline.addLast("packetEncoder", packetEncoder);
            pipeline.addLast("messageDecoder", messageDecoder);
            pipeline.addLast("messageEncoder", messageEncoder);
            pipeline.addLast("dispatcher", connection.dispatcher);
        }
    }

    /**
     *
     */
    public static class Factory
    {
        private final Configuration configuration;
        public final Timer timer = new HashedWheelTimer(new ThreadFactoryBuilder().setNameFormat("Timeouter-%d").build());

        public Factory(Configuration configuration)
        {
            this.configuration = configuration;
        }

        public Bootstrap createNewBootstrap()
        {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_LINGER, 0)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_RCVBUF, null)
                    .option(ChannelOption.SO_SNDBUF, null);
            return bootstrap;
        }
    }

    /**
     *
     */
    private class Dispatcher extends SimpleChannelInboundHandler<Message.Response>
    {
        private final ConcurrentMap<Integer, ResponseHandler> pending = new ConcurrentHashMap<Integer, ResponseHandler>();
        public final StreamIdGenerator streamIdGenerator = new StreamIdGenerator();


        public void addHandler(ResponseHandler handler)
        {
            ResponseHandler oldHandler = pending.put(handler.streamId, handler);
            assert null == oldHandler;
        }

        public void removeHandler(int streamId, boolean release)
        {
            ResponseHandler removed = pending.remove(streamId);
            if (null != removed) {
                removed.cancelTimeout();
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message.Response message) throws Exception
        {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception
        {
            logger.info("Unexpected exception caught {}", e);
            disconnecting(new ClientTransportException(address, String.format("Unexpected exception %s", e.getCause()), e.getCause()));
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
        {
            logger.info("Channel unregistered");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception
        {
            logger.info("Channel closed");
        }


    }

    /**
     *
     */
    private class StreamIdGenerator
    {
        static final int MAX_STREAMS = 128;
        private final AtomicIntegerArray streamsIds = new AtomicIntegerArray(MAX_STREAMS);
        private final AtomicInteger marked = new AtomicInteger(0);

        private StreamIdGenerator()
        {
            for (int i = 0; i < streamsIds.length(); i++) {
                streamsIds.set(i, -1);
            }
        }

        // TODO: resue of streams support
        private AtomicInteger currentId = new AtomicInteger(0);

        public int generate()
        {
            currentId.compareAndSet(Integer.MAX_VALUE, 0);
            iter
            return currentId.incrementAndGet();
        }
    }

    /**
     *
     */
    static class Future extends AbstractFuture<Message.Response> implements RequestHandler.Callback
    {
        private final Message.Request request;
        private volatile InetSocketAddress address;

        Future(Message.Request request)
        {
            this.request = request;
        }

        @Override
        public void register(RequestHandler handler)
        {

        }

        @Override
        public void onSet(Connection connection, Message.Response response, ExecutionInfo info, long latency)
        {
            onSet(connection, response, latency)
        }

        @Override
        public void onSet(Connection connection, Message.Response response, long latency)
        {
            this.address = connection.address;
            super.set(response); // AbstractFuture
        }

        @Override
        public Message.Request request()
        {
            return null;
        }

        @Override
        public void onException(Connection connection, Exception exception, long latency)
        {
            super.setException(exception); // AbstractFuture
        }

        @Override
        public void onTimeout(Connection connection, long latency)
        {
            assert connection != null;
            this.address = connection.address;
            super.setException(new ConnectionException(connection.address, "Operation timed out"));
        }

        public InetSocketAddress getAddress()
        {
            return address;
        }
    }

    /**
     *
     */
    interface ResponseCallback
    {
        public Message.Request request();

        public void onSet(Connection connection, Message.Response response, long latency);

        public void onException(Connection connection, Exception exception, long latency);

        public void onTimeout(Connection connection, long latency);
    }

    /**
     *
     */
    static class ResponseHandler
    {
        public final Connection connection;
        public final ResponseCallback callback;
        public final int streamId;

        private final Timeout timeout;
        private final long startedAt;

        ResponseHandler(Connection connection, ResponseCallback callback)
        {
            this.connection = connection;
            this.callback = callback;
            this.streamId = connection.dispatcher.streamIdGenerator.generate();

            long timeoutMs = connection.factory.configuration.getTimeoutMs();
            this.timeout = connection.factory.timer.newTimeout(newTimeoutTask(), timeoutMs, TimeUnit.MILLISECONDS);

            this.startedAt = System.nanoTime();
        }

        /**
         * Invoked by Dispatcher
         */
        void cancelTimeout()
        {
            if (null != timeout)
                timeout.cancel();
        }

        public void cancelHandler()
        {
            connection.dispatcher.remove(streamId, false);
        }

        private TimerTask newTimeoutTask()
        {
            return new TimerTask()
            {
                @Override
                public void run(Timeout timeout) throws Exception
                {

                }
            }
        }
    }
}
