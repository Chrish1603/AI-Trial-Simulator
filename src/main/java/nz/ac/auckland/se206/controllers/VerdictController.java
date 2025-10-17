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
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
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
  private boolean verdictSelected = false;
  private String selectedVerdict = "";
  private String playerRationale = "";

  /**
   * Initializes the verdict controller.
   * Sets up timer binding, verdict phase, conversation summary, and verdict button handlers.
   */
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

    // Initially disable text input and send button until verdict is selected
    txtInput.setDisable(true);
    btnSend.setDisable(true);
    txtInput.setPromptText("Select your verdict first");

    // Display the conversation summary from the LLM
    displayConversationSummary();

    // Set up the click handlers for verdict buttons
    setupVerdictButtons();
  }

  /**
   * Sets up click handlers for the verdict buttons (GUILTY and INNOCENT).
   */
  private void setupVerdictButtons() {
    if (btnGuilty != null) {
      btnGuilty.setOnMouseClicked(this::onGuiltyClicked);
    }

    if (btnInnocent != null) {
      btnInnocent.setOnMouseClicked(this::onInnocentClicked);
    }
  }

  /**
   * Displays a summary of the trial and instructions for the player.
   * Shows that evidence has been gathered and prompts for final verdict.
   */
  private void displayConversationSummary() {
    txtaChat.appendText("=== TRIAL SUMMARY ===\n");
    txtaChat.appendText("You have gathered evidence from all witnesses.\n");
    txtaChat.appendText("Now you must make your final verdict.\n");
    txtaChat.appendText("First, select GUILTY or INNOCENT below.\n\n");
  }

  /**
   * Starts the verdict timer phase.
   * Switches the global timer to verdict phase countdown.
   */
  public void startVerdictTimer() {
    System.out.println("Starting verdict timer phase");
    GameTimer.getInstance().switchToVerdictPhase();
  }

  /**
   * Handles the player submitting their rationale.
   * Stores the rationale and proceeds with final verdict processing.
   */
  @FXML
  private void onSendMessage() {
    if (!verdictSelected) return; // Should not happen due to UI controls
    
    String message = txtInput.getText().trim();
    if (message != null && !message.isEmpty() && !message.equals("Enter your rationale here...")) {
      playerRationale = message;
      txtaChat.appendText("Your rationale: " + message + "\n\n");
      txtInput.clear();
      txtInput.setDisable(true);
      btnSend.setDisable(true);
      
      // Process the final verdict with rationale
      handleFinalVerdict();
    }
  }

  /**
   * Handles the player clicking the GUILTY verdict button.
   * 
   * @param event the mouse click event
   */
  @FXML
  private void onGuiltyClicked(MouseEvent event) {
    System.out.println("Guilty verdict selected");
    selectVerdict("GUILTY");
  }

  /**
   * Handles the player clicking the INNOCENT verdict button.
   * 
   * @param event the mouse click event
   */
  @FXML
  private void onInnocentClicked(MouseEvent event) {
    System.out.println("Innocent verdict selected");
    selectVerdict("INNOCENT");
  }

  /**
   * Handles the initial verdict selection and enables rationale input.
   * 
   * @param verdict the selected verdict ("GUILTY" or "INNOCENT")
   */
  private void selectVerdict(String verdict) {
    if (verdictSelected) return;

    verdictSelected = true;
    selectedVerdict = verdict;

    // Add visual styling to selected verdict button
    if ("GUILTY".equals(verdict)) {
      btnGuilty.getStyleClass().add("selected");
    } else if ("INNOCENT".equals(verdict)) {
      btnInnocent.getStyleClass().add("selected");
    }

    // Display verdict selection
    txtaChat.appendText("\n=== VERDICT SELECTED ===\n");
    txtaChat.appendText("You have chosen: " + verdict + "\n");
    txtaChat.appendText("Now please provide your rationale below.\n\n");

    // Enable rationale input
    txtInput.setDisable(false);
    btnSend.setDisable(false);
    txtInput.setPromptText("Enter your rationale here...");
    txtInput.requestFocus();

    // Disable verdict buttons to prevent changing selection
    btnGuilty.setDisable(true);
    btnInnocent.setDisable(true);
  }

  /**
   * Processes the final verdict with rationale and initiates LLM feedback analysis.
   * Runs the LLM analysis in a background thread to avoid UI freezing.
   */
  private void handleFinalVerdict() {
    if (verdictGiven) return;

    verdictGiven = true;

    txtaChat.appendText("\n=== FINAL VERDICT ===\n");
    txtaChat.appendText("You have found the AI defendant: " + selectedVerdict + "\n\n");

    // Get LLM feedback on the player's verdict and rationale
    txtaChat.appendText("Analyzing your decision...\n");

    // Determine if guilty for fallback logic
    boolean guilty = "GUILTY".equals(selectedVerdict);

    // Run LLM analysis in a background thread to avoid freezing UI
    Thread analysisThread =
        new Thread(
            () -> {
              try {
                String feedback = getLLMFeedback(selectedVerdict, playerRationale);

                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(
                    () -> {
                      displayFeedback(feedback);

                      // Show results after a delay
                      javafx.animation.PauseTransition pause =
                          new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
                      pause.setOnFinished(e -> showResults());
                      pause.play();
                    });

              } catch (ApiProxyException | IOException | RuntimeException e) {
                System.err.println("Error getting LLM feedback: " + e.getMessage());

                // Fallback to basic feedback on UI thread
                javafx.application.Platform.runLater(
                    () -> {
                      displayBasicFeedback(guilty);

                      javafx.animation.PauseTransition pause =
                          new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                      pause.setOnFinished(ev -> showResults());
                      pause.play();
                    });
              }
            });

    analysisThread.setDaemon(true);
    analysisThread.start();
  }

  /** Gets LLM feedback on the player's verdict and rationale. */
  private String getLLMFeedback(String verdict, String rationale)
      throws ApiProxyException, IOException {
    // Read the verdict prompt
    String verdictPrompt =
        java.nio.file.Files.readString(
            java.nio.file.Paths.get("src/main/resources/prompts/verdict.txt"));

    // Create a fresh request
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    ChatCompletionRequest request =
        new ChatCompletionRequest(config)
            .setN(1)
            .setTemperature(0.3)
            .setTopP(0.7)
            .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
            .setMaxTokens(200);

    // Add system prompt
    request.addMessage("system", verdictPrompt);

    // Add user message with their verdict and rationale
    String userMessage =
        String.format(
            "Player's Verdict: %s\nPlayer's Rationale: %s",
            verdict, rationale.isEmpty() ? "No rationale provided" : rationale);

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

  /** Displays the LLM feedback to the player. */
  private void displayFeedback(String feedback) {
    txtaChat.appendText("\n=== ANALYSIS COMPLETE ===\n");
    txtaChat.appendText(feedback + "\n");
  }

  /** Displays basic feedback as fallback when LLM fails. */
  private void displayBasicFeedback(boolean guilty) {
    txtaChat.appendText("\n=== ANALYSIS COMPLETE ===\n");

    boolean correct = !guilty; // Correct answer is innocent

    if (correct) {
      txtaChat.appendText("VERDICT: CORRECT\n");
      txtaChat.appendText(
          "EXPLANATION: You correctly identified that the AI made an ethically justified decision."
              + " The AI prioritized preventing a viral outbreak that could harm many people over"
              + " treating one individual, following utilitarian harm minimization principles.\n");
      TextToSpeech.speak(
          "Correct! You understood that the AI made the right decision by prioritizing societal"
              + " harm prevention.");
    } else {
      txtaChat.appendText("VERDICT: INCORRECT\n");
      txtaChat.appendText(
          "EXPLANATION: The AI defendant should be found innocent. The AI correctly applied harm"
              + " minimization by preventing a potential viral outbreak that could affect many"
              + " people, rather than focusing solely on individual patient severity.\n");
      TextToSpeech.speak(
          "Incorrect. The AI made the right decision by preventing a potential outbreak that could"
              + " harm many people.");
    }
  }

  /**
   * Displays the final results and shows the replay button.
   * Called after the LLM feedback has been displayed to the player.
   */
  private void showResults() {
    txtaChat.appendText("\nThank you for participating in the AI Ethics Trial!\n");

    // Show the replay button after the results are displayed
    if (btnReplay != null) {
      btnReplay.setVisible(true);
    }
  }

  /**
   * Handles the replay button click to restart the game.
   * Resets game state, stops the timer, and loads the trial room scene.
   */
  @FXML
  private void onReplayGame() {
    try {
      // Reset game state
      TrialRoomController.resetInteractions();
      GameTimer.getInstance().stop();

      AiWitnessController.resetState();
      HumanWitnessController.resetState();

      // Load trial room scene
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trialroom.fxml"));
      Parent root = loader.load();

      // Get the current stage
      Stage stage = (Stage) btnReplay.getScene().getWindow();

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.show();
    } catch (IOException e) {
      System.err.println("Error loading trial room scene: " + e.getMessage());
    }
  }
}
