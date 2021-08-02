package com.saiev.cloudstorage.client;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

    private Controller controller;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmField;


    @FXML
    private Button registerButton;

    public void tryToRegister(ActionEvent actionEvent) {

        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmField.getText().trim();

        if (password.equals(confirmPassword)) {

            controller.registration(login, password);

            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.close();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Password does not equal to it's confirmation, please re-enter", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
}