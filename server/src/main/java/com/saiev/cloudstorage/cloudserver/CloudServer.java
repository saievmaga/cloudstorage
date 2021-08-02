package com.saiev.cloudstorage.cloudserver;

import com.saiev.cloudstorage.cloudserver.handlers.CloudAuthHandler;
import com.saiev.cloudstorage.cloudserver.handlers.CloudStorageHandler;
import com.saiev.cloudstorage.cloudserver.handlers.CommandValidateHandler;
import com.saiev.cloudstorage.cloudserver.handlers.FileReceiverHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class CloudServer {

    public CloudServer() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup workers = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, workers)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new FileReceiverHandler(),
                                    new StringDecoder(),
                                    new StringEncoder(),
                                    new CommandValidateHandler(),
                                    new CloudAuthHandler(),
                                    new CloudStorageHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(5000).sync();
            System.out.println("Сервер запущен");
            future.channel().closeFuture().sync();
            System.out.println("Server finished");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            workers.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
        new CloudServer();
    }
}
