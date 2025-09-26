package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;

public class VerdictController {

    @FXML
private javafx.scene.control.TextArea txtaChat;

@FXML
private javafx.scene.control.TextField txtInput;

@FXML
private javafx.scene.control.Button btnSend;


    @FXML
private void onSendMessage() {
    // Handle sending the message
    String message = txtInput.getText();
    if (message != null && !message.isEmpty()) {
        txtaChat.appendText("You: " + message + "\n");
        txtInput.clear();
        // Optional: trigger AI response here
    }
}
    
}
