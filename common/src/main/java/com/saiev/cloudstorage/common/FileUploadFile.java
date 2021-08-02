package com.saiev.cloudstorage.common;

import java.io.File;
import java.io.Serializable;

public class FileUploadFile  implements Serializable {

    private File file;// файл
    private String fileName;// имя файла
    private int starPos;// начальная позиция
    private byte[] bytes;// байтовый массив файла
    private int endPos;// Конечная позиция

    public int getStarPos() {
        return starPos;
    }

    public void setStarPos(int starPos) {
        this.starPos = starPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
