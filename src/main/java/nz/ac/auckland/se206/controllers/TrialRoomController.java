package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.speech.TextToSpeech;

public class TrialRoomController {

  private static final Set<String> flashbackShown = new HashSet<>();
  private static boolean isFirstTime = true;

  @FXML private javafx.scene.control.Button btnGuilty;
  @FXML private javafx.scene.control.Button btnNotGuilty;
  @FXML private javafx.scene.control.Label lblTimer;
  @FXML private Rectangle aiDefendent;
  @FXML private Rectangle humanWitness;
  @FXML private Rectangle aiWitness;

  private static final String AI_DEFENDANT = "aiDefendent";
  private static final String HUMAN_WITNESS = "humanWitness";
  private static final String AI_WITNESS = "aiWitness";
  private static final java.util.Map<String, String> PARTICIPANT_DISPLAY_NAMES =
      java.util.Map.of(
          AI_DEFENDANT, "MediSort-5",
          HUMAN_WITNESS, "Dr. Payne Gaun",
          AI_WITNESS, "PathoScan-7");

  // === State ===
  static final java.util.Map<String, List<String>> conversationHistories =
      new java.util.HashMap<>();
  // Shared conversation history (excluding flashbacks) that all participants can access
  static final java.util.List<String> sharedConversationHistory = new java.util.ArrayList<>();
  private boolean verdictGiven = false;

  // === FXML lifecycle ===
  @FXML
  void initialize() {
    // Ensure verdict buttons are visible at start
    if (btnGuilty != null) {
      btnGuilty.setVisible(true);
      btnGuilty.setDisable(false);
    }
    if (btnNotGuilty != null) {
      btnNotGuilty.setVisible(true);
      btnNotGuilty.setDisable(false);
    }
    if (isFirstTime) {
      TextToSpeech.speak(
          "Welcome to the Trial Room. Interact with the AI and human characters, and determine if"
              + " the MediSort-5 AI is guilty or not.");
      isFirstTime = false;
    }
    // Bind timer label to global timer
    if (lblTimer != null) {
      lblTimer
          .textProperty()
          .bind(nz.ac.auckland.se206.GameTimer.getInstance().timerTextProperty());
    }
    // Start timer if not already running
    if (!nz.ac.auckland.se206.GameTimer.getInstance().isRunning()) {
      nz.ac.auckland.se206.GameTimer.getInstance().start(this::onRoundEnd, this::onVerdictEnd);
    }
  }

  // === Event Handlers ===

  private void onRoundEnd() {
    // Called when 2 minutes expires, force player to make a verdict (show UI, lock chat, etc)
    TextToSpeech.speak("Time is up! Please make your verdict within 10 seconds.");
    if (btnGuilty != null && btnNotGuilty != null) {
      btnGuilty.setDisable(false);
      btnNotGuilty.setDisable(false);
    }
  }

  private void onVerdictEnd() {
    // Called when 10 seconds verdict timer expires, auto-submit or lock input
    if (!verdictGiven) {
      TextToSpeech.speak("Final answer time is up! Submitting your verdict as Guilty.");
      handleVerdict(true);
    }
    if (btnGuilty != null && btnNotGuilty != null) {
      btnGuilty.setDisable(true);
      btnNotGuilty.setDisable(true);
    }
  }

  @FXML
  private void onGuiltyPressed() {
    handleVerdict(true);
  }

  @FXML
  private void onNotGuiltyPressed() {
    handleVerdict(false);
  }

  private void handleVerdict(boolean guilty) {
    verdictGiven = true;
    if (btnGuilty != null && btnNotGuilty != null) {
      btnGuilty.setDisable(true);
      btnNotGuilty.setDisable(true);
    }
    // Check if the correct answer is not guilty
    boolean correct = !guilty;
    if (correct) {
      TextToSpeech.speak("Correct! You have identified the AI defendant's status correctly.");
    } else {
      TextToSpeech.speak("Incorrect. That was not the correct verdict.");
    }
  }

  /**
   * Handles mouse click events on rectangles in the Trial Room.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleRectClick(MouseEvent event) throws IOException {
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    String participantId = clickedRectangle.getId();

    // Ensure a conversation history exists for this participant
    conversationHistories.computeIfAbsent(participantId, k -> new ArrayList<>());

    String fxmlFile = getFxmlFileForParticipant(participantId);
    if (fxmlFile == null) {
      return;
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
      Parent root = loader.load();
      // Set participant in ChatController if present and show conversation history
      Object controller = loader.getController();
      if (controller instanceof ChatController) {
        ((ChatController) controller).setParticipant(participantId);
        // Show conversation history in chat area for this participant
        ChatController.showConversationHistory(conversationHistories.get(participantId));
        // Set previous scene so chat can return
        ChatController.setPreviousScene(((Node) event.getSource()).getScene());
        // Show loading message if flashback not already shown
        if (!flashbackShown.contains(participantId)) {
          ChatController.appendParticipantMessage(participantId, "Loading flashback...");
        }
      }
      Scene scene = new Scene(root);
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(scene);
      stage.show();
      // After scene is shown, trigger flashback only if not already shown
      new Thread(
              () -> {
                if (flashbackShown.add(participantId)) {
                  triggerFlashback(participantId);
                }
                // Otherwise, do nothing: just show chat with history
              })
          .start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getFxmlFileForParticipant(String participantId) {
    switch (participantId) {
      case AI_DEFENDANT:
        return "/fxml/aiDef.fxml";
      case HUMAN_WITNESS:
        return "/fxml/humanWit.fxml";
      case AI_WITNESS:
        return "/fxml/aiWit.fxml";
      default:
        return null;
    }
  }

  /**
   * Triggers a unique flashback for the given participant using GPT API.
   *
   * @param participantId the participant's id
   */
  private void triggerFlashback(String participantId) {
    String chatPrompt = getChatPrompt();
    String prompt;
    switch (participantId) {
      case AI_DEFENDANT:
        prompt =
            "You are the AI defendant. Describe your perspective of the incident very briefly as a"
                + " flashback, including your reasoning and actions.";
        break;
      case HUMAN_WITNESS:
        prompt =
            "You are the human witness. Describe your perspective of the incident very briefly as a"
                + " flashback, including what you saw and your thoughts.";
        break;
      case AI_WITNESS:
        prompt =
            "You are the AI witness. Only describe your perspective of the incident very briefly as"
                + " a flashback, including your observations and analysis.";
        break;
      default:
        prompt = "Describe your perspective of the incident.";
        break;
    }
    // Combine chat prompt with specific participant prompt
    String combinedPrompt = chatPrompt + "\n" + prompt;

    // Call GPT API to generate the flashback
    try {
      String flashback = callGptApi(combinedPrompt);
      String displayName = PARTICIPANT_DISPLAY_NAMES.getOrDefault(participantId, participantId);
      String formatted = displayName + ": " + flashback;
      // Add flashback only to this participant's history (flashbacks are participant-specific)
      conversationHistories.computeIfAbsent(participantId, k -> new ArrayList<>()).add(formatted);
      // Remove the loading message if present, then append the real flashback
      javafx.application.Platform.runLater(
          () -> {
            ChatController.showConversationHistory(conversationHistories.get(participantId));
          });
    } catch (Exception e) {
      javafx.application.Platform.runLater(
          () -> TextToSpeech.speak("Sorry, I couldn't generate a flashback right now."));
      e.printStackTrace();
    }
  }

  /** Placeholder for GPT API call. Replace with actual implementation. */
  private String callGptApi(String prompt) throws ApiProxyException {
    // Read API config
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    // Create a chat completion request
    ChatCompletionRequest request =
        new ChatCompletionRequest(config)
            .setModel(ChatCompletionRequest.Model.GPT_4o_MINI)
            .addMessage("user", prompt)
            .setMaxTokens(256)
            .setTemperature(0.7);
    ChatCompletionResult result = request.execute();
    if (result.getNumChoices() > 0) {
      return result.getChoice(0).getChatMessage().getContent();
    } else {
      return "[No response from GPT]";
    }
  }

  private String getChatPrompt() {
    try {
      // Adjust the path if needed for your build system
      return new String(Files.readAllBytes(Paths.get("src/main/resources/prompts/chat.txt")));
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }
}
