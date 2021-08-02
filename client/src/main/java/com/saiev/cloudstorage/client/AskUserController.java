package com.saiev.cloudstorage.client;

import com.saiev.cloudstorage.client.Controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AskUserController {

    private Controller controller;

    @FXML
    private Label labelAsk;
    @FXML
    private TextField textFieldAsk;
    @FXML
    private Button btnAskYes;
    @FXML
    private Button btnAskNo;

    private String actionType;

    public void onBtnYesAction(ActionEvent actionEvent) {
        String userInput = textFieldAsk.getText().trim();

        if (actionType.equals("NewDirectory")) {
            controller.createRemoteDirectory(userInput);
        } else if (actionType.equals("NewFile")) {
            controller.createRemoteFile(userInput);
        } else if (actionType.equals("DirectoryDelete")) {
            controller.deleteRemoteDirectory("Y");
        }

        Stage stage = (Stage) textFieldAsk.getScene().getWindow();
        stage.close();
    }

    public void onBtnNoAction(ActionEvent actionEvent) {
        if (actionType.equals("DirectoryDelete")) {
            controller.deleteRemoteDirectory("N");
        }
        Stage stage = (Stage) textFieldAsk.getScene().getWindow();
        stage.close();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void initControls(String question, Boolean isBtnYes, Boolean isBtnNo, String btnYesName, String btnNoName, String actionType, String textEditContent) {
        labelAsk.setText(question);
        btnAskYes.setVisible(isBtnYes);
        btnAskNo.setVisible(isBtnNo);
        btnAskYes.setText(btnYesName);
        btnAskNo.setText(btnNoName);
        this.actionType = actionType;
        if (textEditContent != null && actionType.equals("DirectoryDelete")) {
            textFieldAsk.setText(textEditContent);
            textFieldAsk.setEditable(false);
        }
    }

}