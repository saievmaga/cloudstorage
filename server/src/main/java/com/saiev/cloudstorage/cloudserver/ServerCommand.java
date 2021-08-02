package com.saiev.cloudstorage.cloudserver;

import java.util.ArrayList;
import java.util.List;

/**
 * В классе содержится описание команды, обрабатываемой сервером.
 * Описание содержит наименование и параметры.
 * Используется для проверки на корректность.
 * */
public class ServerCommand {
    private String name;
    private List<String> params;

    public ServerCommand(String name, List<String> params) {
        this.name = name;
        if (params != null) {
            this.params = new ArrayList<>();
            this.params.addAll(params);
        } else {
            this.params = null;
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getParams() {
        return params;
    }

    public int getParamsQuantity() {
        if (params == null) {
            return 0;
        } else {
            return params.size();
        }
    }

    @Override
    public String toString() {
        return "com.saiev.cloudstorage.cloudserver.ServerCommand{" +
                "name='" + name + '\'' +
                ", params=" + params +
                '}';
    }
}
