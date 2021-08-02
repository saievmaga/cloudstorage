package com.saiev.cloudstorage.cloudserver;

import com.saiev.cloudstorage.common.FileInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class StorageLogic {

    private final Path rootPath;  //корневой каталог на сервере, предоставленный подключившемуся пользователю
    private Path currentPath;  //текущий каталог, в котором находится пользователь

    public StorageLogic(Path rootPath) throws IOException {
        this.rootPath = rootPath;
        this.currentPath = rootPath;

        checkUserDirectory();
    }

    private void checkUserDirectory() throws IOException {
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectory(rootPath);
            } catch (IOException e) {
                throw new IOException("USER_DIRECTORY_ERROR");
            }
        }
    }

    public String getUserPath() {
        return rootPath.relativize(currentPath).toString();
    }

    public String getCurrentPath() {
        return currentPath.toString();
    }

    public List<FileInfo> getFilesList() {
        try {
            return Files.list(currentPath).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean createDirectory(String newDir) throws IOException {
        Path newDirectory = Paths.get(currentPath.toString() + File.separator + newDir);
        if (Files.exists(newDirectory)) {
            throw new FileAlreadyExistsException("DIRECTORY_EXISTS_ALREADY");
        } else {
            Files.createDirectory(newDirectory);
            return true;
        }
    }

    public Boolean createFile(String newFileName) throws IOException {
        Path newFile = Paths.get(currentPath.toString() + File.separator + newFileName);
        if (Files.exists(newFile)) {
            throw new FileAlreadyExistsException("FILE_EXISTS_ALREADY");
        } else {
            Files.createFile(newFile);
            return true;
        }
    }

    public void changeDirectory(String newDir) throws IllegalArgumentException {
        if ("~".equals(newDir)) {
            currentPath = rootPath;
        } else if ("..".equals(newDir)) {
            if (!currentPath.equals(rootPath)) {
                currentPath = currentPath.getParent();
            }
        } else {
            Path newPath = Paths.get(currentPath.toString() + File.separator + newDir);
            if (!Files.exists(newPath)) {
                throw new IllegalArgumentException("UNKNOWN_PATH");
            } else {
                currentPath = newPath;
            }
        }
    }

    public Boolean removeFileOrDirectory(String pathToRemove) throws IOException {
        Path deletePath = Paths.get(currentPath.toString() + File.separator + pathToRemove);
        if (!Files.exists(deletePath)) {
            throw new NoSuchFileException("DO_NOT_EXIST");
        } else {
            try {
                Files.delete(deletePath);
            } catch (DirectoryNotEmptyException e) {
                throw new DirectoryNotEmptyException("NOT_EMPTY_DIRECTORY");
            } catch (IOException e) {
                throw new IOException("ERROR");
            }
        }
        return true;
    }

    public void deleteNotEmptyDirectory(String directory) throws IOException {
        String dir = currentPath.toString() + File.separator + directory;
        deleteDirectory(Paths.get(dir));
    }

    private void deleteDirectory(Path directory) throws  IOException {
        String[] files = new File(directory.toString()).list();
        if (files != null && files.length > 0) {
            for (String file : files) {
                Path fileToDelete = Paths.get(directory + File.separator + file);
                if (Files.isDirectory(fileToDelete)) {
                    deleteDirectory(fileToDelete);
                } else {
                    try {
                        Files.delete(fileToDelete);
                    } catch (IOException e) {
                        throw new IOException("ERROR_DELETING_FILE");
                    }
                }
            }
        }
        try {
            Files.delete(directory);
        } catch (IOException e) {
            throw new IOException("ERROR_DELETING_DIRECTORY");
        }
    }

    public void copy (String source, String destination) throws IllegalArgumentException, IOException {
        Path sourcePath = Paths.get(currentPath.toString() + File.separator + source);
        Path destinationPath = Paths.get(currentPath.toString() + File.separator + destination);
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("SOURCE_NOT_FOUND");
        } else {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
