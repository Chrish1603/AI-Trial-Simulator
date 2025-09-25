package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import nz.ac.auckland.se206.GameTimer;

public class VerdictController {

    @FXML
    private TextArea txtaChat;

    @FXML
    private TextField txtInput;

    @FXML
    private Button btnSend;
    
    @FXML
    private Label lblVerdictTimer;
    
    @FXML
    public void initialize() {
        lblVerdictTimer.textProperty().bind(GameTimer.getInstance().timerTextProperty());
        
        if (!GameTimer.getInstance().isInVerdictPhase()) {
            startVerdictTimer();
        }
    }
    
    public void startVerdictTimer() {
        GameTimer.getInstance().start(
            () -> {}, 
            this::handleTimeUp
        );
        
        GameTimer.getInstance().switchToVerdictPhase();
    }
    

    private void handleTimeUp() {
        txtInput.setDisable(true);
        btnSend.setDisable(true);
        
        txtaChat.appendText("\n[TIME UP] Your final verdict has been submitted.\n");
        
        // Optional: Add transition to results screen after a short delay
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> {
            // Transition to results screen or other appropriate screen
            // SceneManager.switchToUi(AppUi.RESULTS); 
            // You'll need to implement this screen or choose an appropriate one
        });
        pause.play();
    }

    @FXML
    private void onSendMessage() {
        String message = txtInput.getText();
        if (message != null && !message.isEmpty()) {
            txtaChat.appendText("You: " + message + "\n");
            txtInput.clear();
            // Optional: trigger AI response here
        }
    }
}
