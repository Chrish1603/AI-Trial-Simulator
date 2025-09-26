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
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
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
    private String playerRationale = "";
    
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
            playerRationale = message;
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
        txtaChat.appendText("You have found the AI defendant: " + verdictText + "\n\n");
        
        // Get LLM feedback on the player's verdict and rationale
        txtaChat.appendText("Analyzing your decision...\n");
        
        // Run LLM analysis in a background thread to avoid freezing UI
        Thread analysisThread = new Thread(() -> {
            try {
                String feedback = getLLMFeedback(verdictText, playerRationale);
                
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    displayFeedback(feedback);
                    
                    // Show results after a delay
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(4));
                    pause.setOnFinished(e -> showResults());
                    pause.play();
                });
                
            } catch (ApiProxyException | IOException | RuntimeException e) {
                System.err.println("Error getting LLM feedback: " + e.getMessage());
                
                // Fallback to basic feedback on UI thread
                javafx.application.Platform.runLater(() -> {
                    displayBasicFeedback(guilty);
                    
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(3));
                    pause.setOnFinished(ev -> showResults());
                    pause.play();
                });
            }
        });
        
        analysisThread.setDaemon(true);
        analysisThread.start();
    }
    
    /**
     * Gets LLM feedback on the player's verdict and rationale.
     */
    private String getLLMFeedback(String verdict, String rationale) throws ApiProxyException, IOException {
        // Read the verdict prompt
        String verdictPrompt = java.nio.file.Files.readString(
            java.nio.file.Paths.get("src/main/resources/prompts/verdict.txt"));
        
        // Create a fresh request
        ApiProxyConfig config = ApiProxyConfig.readConfig();
        ChatCompletionRequest request = new ChatCompletionRequest(config)
            .setN(1)
            .setTemperature(0.3)
            .setTopP(0.7)
            .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
            .setMaxTokens(200);
        
        // Add system prompt
        request.addMessage("system", verdictPrompt);
        
        // Add user message with their verdict and rationale
        String userMessage = String.format(
            "Player's Verdict: %s\nPlayer's Rationale: %s", 
            verdict, 
            rationale.isEmpty() ? "No rationale provided" : rationale);
        
        request.addMessage("user", userMessage);
        
        // Execute request
        ChatCompletionResult result = request.execute();
        
        if (result != null && result.getChoices() != null) {
            for (Choice choice : result.getChoices()) {
                return choice.getChatMessage().getContent();
            }
        }
        
        throw new RuntimeException("No response from LLM");
    }
    
    /**
     * Displays the LLM feedback to the player.
     */
    private void displayFeedback(String feedback) {
        txtaChat.appendText("\n=== ANALYSIS COMPLETE ===\n");
        txtaChat.appendText(feedback + "\n");
        
        // Extract verdict correctness for TTS
        boolean isCorrect = feedback.toUpperCase().contains("VERDICT: CORRECT");
        String ttsMessage;
        
        if (isCorrect) {
            ttsMessage = "Excellent work! Your analysis shows a good understanding of AI ethics and harm minimization principles.";
        } else {
            ttsMessage = "Your verdict needs reconsideration. The key is understanding how AI systems should minimize overall societal harm.";
        }
        
        TextToSpeech.speak(ttsMessage);
    }
    
    /**
     * Displays basic feedback as fallback when LLM fails.
     */
    private void displayBasicFeedback(boolean guilty) {
        txtaChat.appendText("\n=== ANALYSIS COMPLETE ===\n");
        
        boolean correct = !guilty; // Correct answer is innocent
        
        if (correct) {
            txtaChat.appendText("VERDICT: CORRECT\n");
            txtaChat.appendText("EXPLANATION: You correctly identified that the AI made an ethically justified decision. The AI prioritized preventing a viral outbreak that could harm many people over treating one individual, following utilitarian harm minimization principles.\n");
            TextToSpeech.speak("Correct! You understood that the AI made the right decision by prioritizing societal harm prevention.");
        } else {
            txtaChat.appendText("VERDICT: INCORRECT\n");
            txtaChat.appendText("EXPLANATION: The AI defendant should be found innocent. The AI correctly applied harm minimization by preventing a potential viral outbreak that could affect many people, rather than focusing solely on individual patient severity.\n");
            TextToSpeech.speak("Incorrect. The AI made the right decision by preventing a potential outbreak that could harm many people.");
        }
    }

    private void showResults() {
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
