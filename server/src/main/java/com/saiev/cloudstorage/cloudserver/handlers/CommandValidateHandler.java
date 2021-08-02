package com.saiev.cloudstorage.cloudserver.handlers;

import com.saiev.cloudstorage.cloudserver.ServerCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Обработчик предназначен для проверки вводимых команд.
 * Проверяется, что введённая команда известна серверу,
 * а так же наличие необходимого количество параметров
 *
 *
 * */

public class CommandValidateHandler extends SimpleChannelInboundHandler<String> {

    private Map<String, Integer> commandPatterns;

    private ServerCommand getServerCommand(String s) {
        String command = s.replace("\n", "").replace("\r", "");
        String[] cmd = command.split(" ");
        List<String> params = null;
        if (cmd.length > 1) {
            params = Arrays.asList(Arrays.copyOfRange(cmd, 1, cmd.length));
        }
        return new ServerCommand(cmd[0], params);
    }

    private String validate(ServerCommand serverCommand) {
        Integer patternParams = commandPatterns.get(serverCommand.getName());
        if (patternParams == null) {
            return "UNKNOWN_COMMAND";
        } else {
            if (patternParams == serverCommand.getParamsQuantity()) {
                return "SUCCESS";
            } else {
                return "WRONG_PARAMETERS";
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        ServerCommand serverCommand = getServerCommand(s);
        String validateResult = validate(serverCommand);
        if (validateResult.equals("SUCCESS")) {
            ctx.fireChannelRead(serverCommand);
        } else {
            ctx.writeAndFlush(validateResult + "\r\n");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        commandPatterns = new HashMap<>();
        commandPatterns.put("auth", 2);
        commandPatterns.put("reg", 2);
        commandPatterns.put("disconnect", 0);

        commandPatterns.put("ls", 0);
        commandPatterns.put("mkdir", 1);
        commandPatterns.put("touch", 1);
        commandPatterns.put("rm", 1);
        commandPatterns.put("cd", 1);
        commandPatterns.put("copy", 2);

        commandPatterns.put("Y", 0);
        commandPatterns.put("N", 0);

        commandPatterns.put("uploadFile", 2);
        commandPatterns.put("downloadFile", 2);
    }
}
