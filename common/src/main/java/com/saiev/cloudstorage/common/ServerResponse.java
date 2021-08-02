package com.saiev.cloudstorage.common;

import java.io.Serializable;

public class ServerResponse<T> implements Serializable {
    private final com.saiev.cloudstorage.common.ResponseCommand responseCommand;
    private final T responseObject;
    private final String currentPath;

    public ServerResponse(com.saiev.cloudstorage.common.ResponseCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.responseObject = null;
        this.currentPath = null;
    }

    public ServerResponse(com.saiev.cloudstorage.common.ResponseCommand responseCommand, T responseObject) {
        this.responseCommand = responseCommand;
        this.responseObject = responseObject;
        this.currentPath = null;
    }

    public ServerResponse(com.saiev.cloudstorage.common.ResponseCommand responseCommand, T responseObject, String currentPath) {
        this.responseCommand = responseCommand;
        this.responseObject = responseObject;
        this.currentPath = currentPath;
    }

    public com.saiev.cloudstorage.common.ResponseCommand getResponseCommand() {
        return responseCommand;
    }

    public T getResponseObject() {
        return responseObject;
    }

    public String getCurrentPath() { return currentPath; }

}
