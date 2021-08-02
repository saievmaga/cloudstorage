package com.saiev.cloudstorage.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    private Controller controller;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button connectButton;

    public void tryToLogin(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        controller.authentication(login, password);

        Stage stage = (Stage) connectButton.getScene().getWindow();
        stage.close();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
}