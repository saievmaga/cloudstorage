package com.saiev.cloudstorage.common;

import java.io.File;
import java.io.Serializable;

public class FileUpload implements Serializable {

    private File file;
    private long size;

    public FileUpload(File file, long size) {
        this.file = file;
        this.size = size;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }
}
