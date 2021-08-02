package com.saiev.cloudstorage.cloudserver.handlers;

import com.saiev.cloudstorage.common.FileDownload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class FileSenderHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private FileDownload file;
    private Boolean inProcess = false;
    private long bytesSent;
    private RandomAccessFile randomAccessFile;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("File download event triggered");
        if (evt instanceof FileDownload) {
            System.out.println("File download instance");
            file = (FileDownload) evt;
            inProcess = true;
            bytesSent = 0;
            randomAccessFile = new RandomAccessFile(file.getFile(), "r");
            randomAccessFile.seek(0);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (!inProcess) {
            ctx.fireChannelRead(byteBuf);
        }
        else {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);

            String s = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(s);

            randomAccessFile.close();
            file = null;
            inProcess = false;

            /*
            byte[] buffer = new byte[1024];
            int read = randomAccessFile.read(buffer);
            bytesSent += read;
            ByteBuf bb = ctx.alloc().heapBuffer();
            bb.writeBytes(buffer, 0, read);
            ctx.channel().writeAndFlush(bb);

            if (bytesSent == file.getSize()) { //Всё послали, заканчиваем
                randomAccessFile.close();
                file = null;
                inProcess = false;
            }
            */
        }
    }
    /*
    private void sendResponse(ChannelHandlerContext ctx, ServerResponse<?> serverResponse) {
        ByteBuf bb = ctx.alloc().heapBuffer();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(serverResponse);
            bb.writeBytes(baos.toByteArray());
            ctx.channel().writeAndFlush(bb);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    */
}
