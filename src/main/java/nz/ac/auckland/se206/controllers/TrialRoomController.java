package nz.ac.auckland.se206.controllers;

import java.io.IOException;
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
import nz.ac.auckland.se206.speech.TextToSpeech;

public class TrialRoomController {

  private static final Set<String> flashbackShown = new HashSet<>();
  private static boolean isFirstTime = true;
  private static Scene trialRoomScene;

  @FXML private javafx.scene.control.Button btnGuilty;
  @FXML private javafx.scene.control.Button btnNotGuilty;
  @FXML private javafx.scene.control.Label lblTimer;
  @FXML private Rectangle aiDefendent;
  @FXML private Rectangle humanWitness;
  @FXML private Rectangle aiWitness;

  private static final String AI_DEFENDANT = "aiDefendent";
  private static final String HUMAN_WITNESS = "humanWitness";
  private static final String AI_WITNESS = "aiWitness";

  // === State ===
  static final java.util.Map<String, List<String>> conversationHistories =
      new java.util.HashMap<>();
  // Shared conversation history (excluding flashbacks) that all participants can access
  static final java.util.List<String> sharedConversationHistory = new java.util.ArrayList<>();
  private boolean verdictGiven = false;

  // === FXML lifecycle ===
  @FXML
  void initialize() {
    // Store reference to trial room scene
    javafx.application.Platform.runLater(() -> {
      if (btnGuilty != null) {
        trialRoomScene = btnGuilty.getScene();
      }
    });
    
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

    // Check if flashback should be shown first
    if (!flashbackShown.contains(participantId)) {
      showFlashback(participantId, event);
      flashbackShown.add(participantId);
      return;
    }

    // Otherwise, go directly to chat
    showChatInterface(participantId, event);
  }

  /**
   * Shows the flashback for the given participant
   */
  private void showFlashback(String participantId, MouseEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/flashback.fxml"));
    Parent root = loader.load();
    
    FlashbackController controller = loader.getController();
    String returnFxml = getFxmlFileForParticipant(participantId);
    controller.initializeFlashback(participantId, returnFxml);
    
    Scene scene = new Scene(root);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.show();
  }

  /**
   * Shows the chat interface for the given participant
   */
  private void showChatInterface(String participantId, MouseEvent event) throws IOException {
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
      }
      
      Scene scene = new Scene(root);
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      stage.setScene(scene);
      stage.show();
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
   * Gets the trial room scene for returning from flashback
   */
  public static Scene getTrialRoomScene() {
    return trialRoomScene;
  }
}
