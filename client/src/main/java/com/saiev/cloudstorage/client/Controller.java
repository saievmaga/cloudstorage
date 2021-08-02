package com.saiev.cloudstorage.client;


import com.saiev.cloudstorage.client.AskUserController;
import com.saiev.cloudstorage.client.LoginController;
import com.saiev.cloudstorage.client.RegisterController;
import com.saiev.cloudstorage.common.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 5000;

    private boolean authenticated;
    private CloudUser cloudUser;

    private final Path initPath = Paths.get(".");

    @FXML
    private Label labelInfo;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnRegister;
    @FXML
    private Button btnLogout;
    @FXML
    private TableView<FileInfo> remoteTable;
    @FXML
    private TextField remotePathField;
    @FXML
    private Button btnUp;
    @FXML
    private Button btnRoot;
    @FXML
    private TableView<FileInfo> clientTable;
    @FXML
    private ComboBox<String> driveBox;
    @FXML
    private TextField pathField;
    @FXML
    private Button btnNewDir;
    @FXML
    private Button btnNewFile;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnUpload;
    @FXML
    private Button btnDownload;

    private Stage stage;

    private Stage loginStage;
    private LoginController loginController;

    private Stage registerStage;
    private RegisterController registerController;

    private Stage askUserStage;
    private AskUserController askUserController;

    public void initTableView(TableView<FileInfo> table) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileType().getName()));
        fileTypeColumn.setPrefWidth(30);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(300);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(50);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[Dir]";
                        }
                        setText(text);
                    }
                }
            };

        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("modified");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileSizeColumn.setPrefWidth(50);

        table.getColumns().add(fileTypeColumn);
        table.getColumns().add(fileNameColumn);
        table.getColumns().add(fileSizeColumn);
        table.getColumns().add(fileDateColumn);
        table.getSortOrder().add(fileTypeColumn);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTableView(clientTable);
        clientTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        getLocalFiles(path);
                    }
                }
            }
        });
        clientTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    Path path = Paths.get(pathField.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        getLocalFiles(path);
                    }
                }
            }
        });

        driveBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            driveBox.getItems().add(p.toString());
        }
        driveBox.getSelectionModel().select(initPath.toAbsolutePath().getRoot().toString());
        getLocalFiles(initPath);

        initTableView(remoteTable);
        remoteTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    if (remoteTable.getSelectionModel().getSelectedItem().getFileType() == FileInfo.FileType.DIRECTORY) {
                        changeRemoteDirectory(remoteTable.getSelectionModel().getSelectedItem().getFileName());
                    }
                }
            }
        });
        remoteTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    if (remoteTable.getSelectionModel().getSelectedItem().getFileType() == FileInfo.FileType.DIRECTORY) {
                        changeRemoteDirectory(remoteTable.getSelectionModel().getSelectedItem().getFileName());
                    }
                }
            }
        });

        Platform.runLater(() -> {
            stage = (Stage) clientTable.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()) {
                    sendCommand("disconnect");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            in.close();
            out.close();
            socket.close();
            cloudUser = null;
            remoteTable.getItems().clear();
            remotePathField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToLogin(ActionEvent actionEvent) {
        if (loginStage != null) {
            loginStage = null;
        }
        createLoginWindow();
        Platform.runLater(() -> loginStage.show());
    }

    private void createLoginWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = fxmlLoader.load();
            loginStage = new Stage();
            loginStage.setTitle("Authentication");
            loginStage.setScene(new Scene(root, 225, 100));

            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.initStyle(StageStyle.UTILITY);

            loginController = fxmlLoader.getController();
            loginController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToRegister(ActionEvent actionEvent) {
        if (registerStage != null) {
            registerStage = null;
        }
        createRegisterWindow();
        Platform.runLater(() -> registerStage.show());
    }

    private void createRegisterWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = fxmlLoader.load();
            registerStage = new Stage();
            registerStage.setTitle("Registration");
            registerStage.setScene(new Scene(root, 225, 125));

            registerStage.initModality(Modality.APPLICATION_MODAL);
            registerStage.initStyle(StageStyle.UTILITY);

            registerController = fxmlLoader.getController();
            registerController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object readObject() {
        Object o = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(in);
            o = ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return o;
    }

    public void authentication(String login, String password) {

        sendCommand(String.format("auth %s %s", login, password));

        Object o = readObject();
        if (o instanceof ServerResponse) {
            ServerResponse<CloudUser> sr = (ServerResponse<CloudUser>) o;
            if (sr.getResponseCommand() == ResponseCommand.AUTH_OK) {
                cloudUser = sr.getResponseObject();
            } else if (sr.getResponseCommand() == ResponseCommand.AUTH_FAIL) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Incorrect login or password", ButtonType.OK);
                alert.showAndWait();
                cloudUser = null;
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Authentication error", ButtonType.OK);
            alert.showAndWait();
            cloudUser = null;
        }

        setAuthenticated(cloudUser != null);
    }

    public void registration(String login, String password) {

        sendCommand(String.format("reg %s %s", login, password));

        Object o = readObject();
        if (o instanceof ServerResponse) {
            ServerResponse<CloudUser> sr = (ServerResponse<CloudUser>) o;
            if (sr.getResponseCommand() == ResponseCommand.REG_OK) {
                cloudUser = sr.getResponseObject();
            } else if (sr.getResponseCommand() == ResponseCommand.REG_FAIL) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Registration error", ButtonType.OK);
                alert.showAndWait();
                cloudUser = null;
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Registration error", ButtonType.OK);
            alert.showAndWait();
            cloudUser = null;
        }
        setAuthenticated(cloudUser != null);
    }

    private void sendCommand(String command) {

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        setUserStatus();
    }

    public void tryToLogout(ActionEvent actionEvent) {
        sendCommand("disconnect");

        Object o = readObject();
        if (o instanceof ServerResponse) {
            ServerResponse<CloudUser> sr = (ServerResponse<CloudUser>) o;
            if (sr.getResponseCommand() == ResponseCommand.AUTH_OUT) {
                disconnect();
            }
        }
        setAuthenticated(cloudUser != null);
    }

    private void setUserStatus() {
        if (!authenticated) {
            labelInfo.setText("SERVER SIDE, user not connected");
            labelInfo.setStyle("-fx-font-weight: normal;");
        } else {
            if (cloudUser != null) {
                labelInfo.setText("SERVER SIDE, user connected : " + cloudUser.getLogin());
                getRemoteFiles();
            } else {
                labelInfo.setText("SERVER SIDE, user connected : ERROR");
            }
            labelInfo.setStyle("-fx-font-weight: bold;");
        }
        btnLogin.setDisable(authenticated);
        btnRegister.setDisable(authenticated);
        btnLogout.setDisable(!authenticated);
        btnUp.setDisable(!authenticated);
        btnRoot.setDisable(!authenticated);
        btnNewDir.setDisable(!authenticated);
        btnNewFile.setDisable(!authenticated);
        btnDelete.setDisable(!authenticated);
        btnUpload.setDisable(!authenticated);
        btnDownload.setDisable(!authenticated);
    }

    @FXML
    private void selectDriveAction(ActionEvent actionEvent) {
        ComboBox<String> cb = (ComboBox<String>) actionEvent.getSource();
        getLocalFiles(Paths.get(cb.getSelectionModel().getSelectedItem()));
    }

    @FXML
    private void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            getLocalFiles(upperPath);
        }
    }

    private void getLocalFiles(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientTable.sort();
            clientTable.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Unable to get file's list", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void getRemoteFiles() {
        remoteTable.getItems().clear();

        if (authenticated) {
            sendCommand("ls");
            Object o = readObject();
            if (o instanceof ServerResponse) {
                ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
                if (sr.getResponseCommand() == ResponseCommand.FILES_LIST) {
                    remoteTable.getItems().addAll(sr.getResponseObject());
                    remoteTable.sort();
                    remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Error while getting file's list", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
    }

    private void changeRemoteDirectory(String newDir) {
        sendCommand(String.format("cd %s", newDir));
        Object o = readObject();
        if (o instanceof ServerResponse) {
            ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
            if (sr.getResponseCommand() == ResponseCommand.FILES_CD_OK) {
                remoteTable.getItems().clear();
                if (sr.getResponseObject() != null) {
                    if (sr.getResponseObject().size() > 0) {
                        remoteTable.getItems().addAll(sr.getResponseObject());
                        remoteTable.sort();
                    }
                }
                remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());
            } else if (sr.getResponseCommand() == ResponseCommand.FILES_CD_FAIL) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Error while changing directory", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    public void cdRemoteDirUp(ActionEvent actionEvent) {
        changeRemoteDirectory("..");
    }

    public void cdRemoteDirRoot(ActionEvent actionEvent) {
        changeRemoteDirectory("~");
    }

    private void createAskUserWindow(String question, Boolean isBtnYes, Boolean isBtnNo, String btnYesName, String btnNoName,
                                     String actionType, String textEditContent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/askuser.fxml"));
            Parent root = fxmlLoader.load();
            askUserStage = new Stage();
            askUserStage.setTitle("Question");
            askUserStage.setScene(new Scene(root, 500, 70));

            askUserStage.initModality(Modality.APPLICATION_MODAL);
            askUserStage.initStyle(StageStyle.UTILITY);

            askUserController = fxmlLoader.getController();
            askUserController.setController(this);
            askUserController.initControls(question, isBtnYes, isBtnNo, btnYesName, btnNoName, actionType, textEditContent);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToCreateDir(ActionEvent actionEvent) {
        if (askUserStage != null) {
            askUserStage = null;
        }
        createAskUserWindow("New directory name ", true, true, "OK", "CANCEL", "NewDirectory", null);
        Platform.runLater(() -> askUserStage.show());
    }

    public void createRemoteDirectory(String newDirectory) {
        if (newDirectory.length() > 0) {
            sendCommand(String.format("mkdir %s", newDirectory));
            Object o = readObject();
            if (o instanceof ServerResponse) {
                ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
                if (sr.getResponseCommand() == ResponseCommand.FILES_MKDIR_OK) {
                    remoteTable.getItems().clear();
                    if (sr.getResponseObject() != null) {
                        if (sr.getResponseObject().size() > 0) {
                            remoteTable.getItems().addAll(sr.getResponseObject());
                            remoteTable.sort();
                        }
                    }
                    remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());
                } else if (sr.getResponseCommand() == ResponseCommand.FILES_MKDIR_ALREADY_EXISTS) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Directory exists already", ButtonType.OK);
                    alert.showAndWait();
                } else if (sr.getResponseCommand() == ResponseCommand.FILES_MKDIR_FAIL) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Error while creating new directory", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "New directory's name is empty", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void tryToCreateFile(ActionEvent actionEvent) {
        if (askUserStage != null) {
            askUserStage = null;
        }
        createAskUserWindow("New file name ", true, true, "OK", "CANCEL", "NewFile", null);
        Platform.runLater(() -> askUserStage.show());
    }

    public void createRemoteFile(String newFile) {
        if (newFile.length() > 0) {
            sendCommand(String.format("touch %s", newFile));
            Object o = readObject();
            if (o instanceof ServerResponse) {
                ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
                if (sr.getResponseCommand() == ResponseCommand.FILES_TOUCH_OK) {
                    remoteTable.getItems().clear();
                    if (sr.getResponseObject() != null) {
                        if (sr.getResponseObject().size() > 0) {
                            remoteTable.getItems().addAll(sr.getResponseObject());
                            remoteTable.sort();
                        }
                    }
                    remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());
                } else if (sr.getResponseCommand() == ResponseCommand.FILES_TOUCH_ALREADY_EXISTS) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "File exists already", ButtonType.OK);
                    alert.showAndWait();
                } else if (sr.getResponseCommand() == ResponseCommand.FILES_TOUCH_FAIL) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Error while creating new file", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "New file's name is empty", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void tryToDelete(ActionEvent actionEvent) {
        if (remoteTable.getSelectionModel().getSelectedItem() != null) {
            FileInfo.FileType ft = remoteTable.getSelectionModel().getSelectedItem().getFileType();
            sendCommand(String.format("rm %s", remoteTable.getSelectionModel().getSelectedItem().getFileName()));
            Object o = readObject();
            if (o instanceof ServerResponse) {
                ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
                if (sr.getResponseCommand() == ResponseCommand.FILES_RM_OK) {
                    remoteTable.getItems().clear();
                    if (sr.getResponseObject() != null) {
                        if (sr.getResponseObject().size() > 0) {
                            remoteTable.getItems().addAll(sr.getResponseObject());
                            remoteTable.sort();
                        }
                    }
                    remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());

                } else if (sr.getResponseCommand() == ResponseCommand.FILES_RM_NOT_EXISTS) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, (ft == FileInfo.FileType.DIRECTORY) ? "Directory" : "File" + "exists already", ButtonType.OK);
                    alert.showAndWait();

                } else if (sr.getResponseCommand() == ResponseCommand.FILES_MKDIR_FAIL) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Error while creating new " + ((ft == FileInfo.FileType.DIRECTORY) ? "Directory" : "File"), ButtonType.OK);
                    alert.showAndWait();

                } else if (sr.getResponseCommand() == ResponseCommand.FILES_RM_DELETE_DIR) {
                    if (askUserStage != null) {
                        askUserStage = null;
                    }
                    createAskUserWindow("Directory is not empty. Do you want to delete it anyway? ",
                            true, true, "YES", "NO",
                            "DirectoryDelete", remoteTable.getSelectionModel().getSelectedItem().getFileName());

                    Platform.runLater(() -> askUserStage.show());
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Choose file or directory to delete", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void deleteRemoteDirectory(String userAnswer) {
        sendCommand(userAnswer);
        Object o = readObject();
        if (o instanceof ServerResponse) {
            ServerResponse<List<FileInfo>> sr = (ServerResponse<List<FileInfo>>) o;
            if (sr.getResponseCommand() == ResponseCommand.FILES_RM_OK) {
                remoteTable.getItems().clear();
                if (sr.getResponseObject() != null) {
                    if (sr.getResponseObject().size() > 0) {
                        remoteTable.getItems().addAll(sr.getResponseObject());
                        remoteTable.sort();
                    }
                }
                remotePathField.setText(cloudUser.getUserDirectory() + File.separator + sr.getCurrentPath());

            } else if (sr.getResponseCommand() == ResponseCommand.FILES_RM_FAIL) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Error while deleting directory", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    private void uploadFile(String fileName, long size) {
        sendCommand(String.format("uploadFile %s %s", fileName, size));

        File file = new File(pathField.getText() + File.separator + fileName);
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(0);

            //long size = randomAccessFile.length();
            byte[] buffer = new byte[1024];

            for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) {
                int read = randomAccessFile.read(buffer);
                out.write(buffer, 0, read);
            }
            randomAccessFile.close();

            Object o = readObject();
            if (o instanceof ServerResponse) {
                ServerResponse sr = (ServerResponse) o;
                if (sr.getResponseCommand() == ResponseCommand.FILE_UPLOAD_SUCCESS) {
                    getRemoteFiles();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Upload file completed successfully", ButtonType.OK);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Upload file error", ButtonType.OK);
                    alert.showAndWait();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToUploadFile(ActionEvent actionEvent) {
        if (clientTable.getSelectionModel().getSelectedItem() != null) {
            FileInfo fi = clientTable.getSelectionModel().getSelectedItem();
            if (fi.getFileType() == FileInfo.FileType.FILE) {
                uploadFile(fi.getFileName(), fi.getSize());
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Choose file or directory to upload to server", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void tryToDownloadFile(ActionEvent actionEvent) {
        if (remoteTable.getSelectionModel().getSelectedItem() != null) {
            FileInfo fi = remoteTable.getSelectionModel().getSelectedItem();
            if (fi.getFileType() == FileInfo.FileType.FILE) { //Пока сделаем только для файла, каталог позднее
                /*
                sendCommand(String.format("downloadFile %s %s", fi.getFileName(), fi.getSize()));
                try {
                    out.write("get_file_chunk".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Choose file or directory to delete", ButtonType.OK);
            alert.showAndWait();
        }
    }
}