import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class _NettyServer {

    public _NettyServer() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup workers = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, workers)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new StringEncoder(),
                                    new StringDecoder(),
                                    (ChannelHandler) new CommandValidateHandler()
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
        new _NettyServer();
    }
}
