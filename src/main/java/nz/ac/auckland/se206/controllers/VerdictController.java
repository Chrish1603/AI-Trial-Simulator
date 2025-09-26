package nz.ac.auckland.se206.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import nz.ac.auckland.se206.GameTimer;
import nz.ac.auckland.se206.speech.TextToSpeech;

public class VerdictController {

    @FXML private TextArea txtaChat;
    @FXML private TextField txtInput;
    @FXML private Button btnSend;
    @FXML private Button btnReplay;
    @FXML private Label lblVerdictTimer;
    @FXML private Rectangle btnGuilty;
    @FXML private Rectangle btnInnocent;
    
    private boolean verdictGiven = false;
    
    @FXML
    public void initialize() {
        System.out.println("VerdictController initialized");
        
        // Bind the timer label to the global timer
        if (lblVerdictTimer != null) {
            lblVerdictTimer.textProperty().bind(GameTimer.getInstance().timerTextProperty());
        }
        
        // Make sure we're in verdict phase
        if (!GameTimer.getInstance().isInVerdictPhase()) {
            startVerdictTimer();
        }
        
        // Display conversation summary
        displayConversationSummary();
        
        // Set up the click handlers for verdict buttons
        setupVerdictButtons();
    }
    
    private void setupVerdictButtons() {
        if (btnGuilty != null) {
            btnGuilty.setOnMouseClicked(this::onGuiltyClicked);
        }
        
        if (btnInnocent != null) {
            btnInnocent.setOnMouseClicked(this::onInnocentClicked);
        }
    }
    
    private void displayConversationSummary() {
        txtaChat.appendText("=== TRIAL SUMMARY ===\n");
        txtaChat.appendText("You have gathered evidence from all witnesses.\n");
        txtaChat.appendText("Now you must make your final verdict.\n");
        txtaChat.appendText("Enter your rationale and select GUILTY or INNOCENT.\n\n");
    }
    
    public void startVerdictTimer() {
        System.out.println("Starting verdict timer phase");
        GameTimer.getInstance().switchToVerdictPhase();
    }

    @FXML
    private void onSendMessage() {
        String message = txtInput.getText().trim();
        if (message != null && !message.isEmpty() && !message.equals("Enter your rationale here:")) {
            txtaChat.appendText("Your rationale: " + message + "\n\n");
            txtInput.clear();
            txtInput.setPromptText("Rationale submitted. Now select your verdict.");
        }
    }
    
    @FXML
    private void onGuiltyClicked(MouseEvent event) {
        System.out.println("Guilty verdict selected");
        handleVerdict(true);
    }
    
    @FXML
    private void onInnocentClicked(MouseEvent event) {
        System.out.println("Innocent verdict selected");
        handleVerdict(false);
    }
    
    private void handleVerdict(boolean guilty) {
        if (verdictGiven) return;
        
        verdictGiven = true;
        txtInput.setDisable(true);
        btnSend.setDisable(true);
        
        String verdictText = guilty ? "GUILTY" : "INNOCENT";
        txtaChat.appendText("\n=== FINAL VERDICT ===\n");
        txtaChat.appendText("You have found the AI defendant: " + verdictText + "\n");
        
        // Check if the correct answer is innocent (not guilty)
        boolean correct = !guilty;
        if (correct) {
            txtaChat.appendText("\nCORRECT! The AI made a statistically sound decision to minimize harm.\n");
            TextToSpeech.speak("Correct! You have identified that the AI defendant is innocent. The AI made a statistically sound decision to minimize overall harm.");
        } else {
            txtaChat.appendText("\nINCORRECT. The AI's decision was ethically justified based on harm minimization.\n");
            TextToSpeech.speak("Incorrect. The AI defendant is actually innocent. The AI's decision was ethically justified based on harm minimization principles.");
        }
        
        // Show results after a delay
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> showResults());
        pause.play();
    }
    
    private void showResults() {
        // You can implement a results screen here or restart the game
        txtaChat.appendText("\nThank you for participating in the AI Ethics Trial!\n");
        
        // Show the replay button after the results are displayed
        if (btnReplay != null) {
            btnReplay.setVisible(true);
        }
    }
    
    @FXML
    private void onReplayGame() {
        try {
            // Reset game state
            TrialRoomController.resetInteractions();
            GameTimer.getInstance().stop();
            
            // Load trial room scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trialroom.fxml"));
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) btnReplay.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
