package com.saiev.cloudstorage.cloudserver.handlers;


import com.saiev.cloudstorage.common.FileUpload;
import com.saiev.cloudstorage.common.ResponseCommand;
import com.saiev.cloudstorage.common.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;


public class FileReceiverHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private FileUpload file;
    private Boolean inProcess = false;
    private long bytesRed;
    private RandomAccessFile randomAccessFile;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof FileUpload) {
            file = (FileUpload) evt;
            inProcess = true;
            bytesRed = 0;
            randomAccessFile = new RandomAccessFile(file.getFile(), "rw");
            randomAccessFile.seek(0);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (!inProcess) { //Хэндлер первый в списке, если загрузка файла не в процессе, то прокидываем дальше
            ctx.fireChannelRead(byteBuf.retain());
        } else {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            bytesRed += bytes.length;
            randomAccessFile.write(bytes);

            if (bytesRed == file.getSize()) { //Всё считали, заканчиваем
                randomAccessFile.close();
                file = null;
                inProcess = false;

                sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILE_UPLOAD_SUCCESS));
            }
        }
    }

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
}

