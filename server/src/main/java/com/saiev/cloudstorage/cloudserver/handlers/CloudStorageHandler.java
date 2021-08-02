package com.saiev.cloudstorage.cloudserver.handlers;


import com.saiev.cloudstorage.cloudserver.CloudUserCommand;
import com.saiev.cloudstorage.cloudserver.StorageLogic;
import com.saiev.cloudstorage.common.FileUpload;
import com.saiev.cloudstorage.common.ResponseCommand;
import com.saiev.cloudstorage.common.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;


public class CloudStorageHandler extends SimpleChannelInboundHandler<CloudUserCommand> {

    private final String serverStorageUserData = "server" + File.separator + "Storage" + File.separator + "UserData";
    private StorageLogic storageLogic;
    private static String lastCommandWithResponseRequest;
    private static String lastCommandParam;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudUserCommand s) throws Exception {

        try {
            if (storageLogic == null) {
                storageLogic = new StorageLogic(Paths.get(serverStorageUserData + File.separator + s.getUser().getUserDirectory()));
            }

            switch (s.getCommand().getName()) {

                case "ls":
                    sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_LIST, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    break;

                case "mkdir":
                    try {
                        if (storageLogic.createDirectory(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (FileAlreadyExistsException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_ALREADY_EXISTS));
                    } catch (Exception e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_FAIL));
                    }
                    break;

                case "touch":
                    try {
                        if (storageLogic.createFile(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (FileAlreadyExistsException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_ALREADY_EXISTS));
                    } catch (Exception e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_FAIL));
                    }

                    break;

                case "cd":
                    try {
                        storageLogic.changeDirectory(s.getCommand().getParams().get(0));
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_CD_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    } catch (IllegalArgumentException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_CD_FAIL));
                    }
                    break;

                case "rm":
                    lastCommandWithResponseRequest = "rm";
                    lastCommandParam = s.getCommand().getParams().get(0);
                    try {
                        if (storageLogic.removeFileOrDirectory(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (DirectoryNotEmptyException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_DELETE_DIR));
                    } catch (NoSuchFileException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_NOT_EXISTS));
                    } catch (IOException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_FAIL));
                    }
                    break;

                case "N":
                    if (lastCommandWithResponseRequest != null) {
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    }
                    break;

                case "Y":
                    if (lastCommandWithResponseRequest != null && lastCommandWithResponseRequest.equals("rm")) {
                        try {
                            storageLogic.deleteNotEmptyDirectory(lastCommandParam);
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));

                        } catch (IOException e) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_FAIL));
                        }
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                    }
                    break;

                case "copy":
                    //storageLogic.copy(s.getCommand().getParams().get(0), s.getCommand().getParams().get(1));
                    break;

                case "uploadFile":
                    File fileUp = new File(storageLogic.getCurrentPath() + File.separator + s.getCommand().getParams().get(0));
                    FileUpload fu = new FileUpload(fileUp, Long.parseLong(s.getCommand().getParams().get(1)));
                    if (fu.getSize() != 0) {
                        ctx.channel().pipeline().fireUserEventTriggered(fu);
                    }
                    break;

                case "downloadFile":
                    /*
                    System.out.println("CloudStorageHandler : downloadFile");
                    File fileDown = new File(storageLogic.getCurrentPath() + File.separator + s.getCommand().getParams().get(0));
                    FileDownload fd = new FileDownload(fileDown, Long.parseLong(s.getCommand().getParams().get(1)));
                    ctx.channel().pipeline().fireUserEventTriggered(fd);
                    */
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
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
