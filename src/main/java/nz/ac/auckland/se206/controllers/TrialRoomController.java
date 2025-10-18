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
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import nz.ac.auckland.se206.speech.TextToSpeech;

public class TrialRoomController {

  private static final Set<String> flashbackShown = new HashSet<>();
  private static final Set<String> chatboxesInteracted = new HashSet<>(); // Track interactions
  private static boolean isFirstTime = true;
  private static Scene trialRoomScene;

  @FXML private Button btnVerdict;
  @FXML private Button btnGuilty;
  @FXML private Button btnNotGuilty;
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
  /**
   * Initializes the Trial Room scene, setting up UI components, timer bindings, and game state.
   * This method is automatically called when the FXML is loaded.
   */
  @FXML
  void initialize() {
    // Store reference to trial room scene
    javafx.application.Platform.runLater(
        () -> {
          if (btnVerdict != null) {
            trialRoomScene = btnVerdict.getScene();

            // Store current stage for timer transitions
            if (trialRoomScene != null
                && trialRoomScene.getWindow() instanceof Stage) {
              nz.ac.auckland.se206.GameTimer.getInstance()
                  .setCurrentStage((Stage) trialRoomScene.getWindow());
            }
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

    // Initially disable verdict button until all chatboxes are interacted with
    if (btnVerdict != null) {
      btnVerdict.setDisable(true);
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
          .bind(nz.ac.auckland.se206.GameTimer.getInstance().getTimerTextProperty());
    }

    // Start timer if not already running
    if (!nz.ac.auckland.se206.GameTimer.getInstance().isRunning()) {
      nz.ac.auckland.se206.GameTimer.getInstance().start(this::onRoundEnd, this::onVerdictEnd);
    }

    // Check if we should enable the verdict button (if returning to this scene)
    updateVerdictButtonState();
  }

  // === Event Handlers ===

  /**
   * Called when the 5-minute round timer expires. 
   * Checks if all three chatboxes have been interacted with to determine next action.
   */
  private void onRoundEnd() {
    // Called when 5 minutes expires
    System.out.println("Round timer ended. Chatboxes interacted: " + chatboxesInteracted.size());

    if (chatboxesInteracted.size() >= 3) {
      // All three chatboxes interacted with - proceed to verdict
      TextToSpeech.speak("Time is up! You've gathered enough evidence. Proceeding to verdict.");

      // Use Platform.runLater to ensure UI updates happen on JavaFX thread
      javafx.application.Platform.runLater(
          () -> {
            try {
              handleVerdictButton();
            } catch (Exception e) {
              System.err.println("Failed to switch to verdict scene:");
              e.printStackTrace();
            }
          });
    } else {
      // Not all chatboxes interacted with - game over
      TextToSpeech.speak(
          "Time is up! You didn't gather enough evidence from all witnesses. Game Over.");

      // Use Platform.runLater for UI updates
      javafx.application.Platform.runLater(
          () -> {
            try {
              showGameOverScreen();
            } catch (Exception e) {
              System.err.println("Failed to show game over screen:");
              e.printStackTrace();
            }
          });
    }
  }

  /**
   * Called when the verdict timer expires. Auto-submits the verdict as guilty if no verdict was given.
   */
  private void onVerdictEnd() {
    // Called when verdict timer expires, auto-submit or lock input
    if (!verdictGiven) {
      TextToSpeech.speak("Final answer time is up! Submitting your verdict as Guilty.");
      handleVerdict(true);
    }
    if (btnGuilty != null && btnNotGuilty != null) {
      btnGuilty.setDisable(true);
      btnNotGuilty.setDisable(true);
    }
  }

  /**
   * Displays the game over screen when the player fails to gather enough evidence.
   */
  private void showGameOverScreen() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameover.fxml"));
      Parent root = loader.load();

      // Get the current stage
      Stage stage = (Stage) lblTimer.getScene().getWindow();

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles the "Guilty" button press event.
   */
  @FXML
  private void onGuiltyPressed() {
    handleVerdict(true);
  }

  /**
   * Handles the "Not Guilty" button press event.
   */
  @FXML
  private void onNotGuiltyPressed() {
    handleVerdict(false);
  }

  /**
   * Handles the verdict logic after a button is pressed.
   * 
   * @param guilty true if the verdict is guilty, false if not guilty
   */
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

    System.out.println("Opening chatbox: " + participantId);

    // Update verdict button state
    updateVerdictButtonState();

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
   * Updates the verdict button's visual state and enabled status based on chatbox interactions.
   */
  private void updateVerdictButtonState() {
    if (btnVerdict != null) {
      boolean allChatboxesInteracted = chatboxesInteracted.size() >= 3;
      btnVerdict.setDisable(!allChatboxesInteracted);

      if (allChatboxesInteracted) {
        btnVerdict.setStyle("-fx-background-color: #2ecc71;"); // Green when enabled
      } else {
        btnVerdict.setStyle("-fx-background-color: #95a5a6;"); // Gray when disabled
      }
    }
  }

  /**
   * Switches to the verdict scene if all chatboxes have been interacted with.
   */
  @FXML
  private void handleVerdictButton() {
    // Only allow switching to verdict if all chatboxes have been interacted with
    if (chatboxesInteracted.size() < 3) {
      TextToSpeech.speak("You need to interview all witnesses first.");
      return;
    }

    try {
      System.out.println("Switching to verdict scene...");
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verdict.fxml"));
      Parent root = loader.load();

      // Get the current stage
      Stage stage = (Stage) lblTimer.getScene().getWindow();

      Scene scene = new Scene(root);
      stage.setScene(scene);
      stage.show();

      // Access the controller to ensure verdict timer starts
      VerdictController controller = loader.getController();
      if (controller != null) {
        controller.startVerdictTimer();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Shows the flashback for the given participant.
   * 
   * @param participantId the ID of the participant whose flashback to show
   * @param event the mouse event that triggered this action
   * @throws IOException if there is an error loading the flashback FXML
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
   * Shows the chat interface for the given participant.
   * 
   * @param participantId the ID of the participant to chat with
   * @param event the mouse event that triggered this action
   * @throws IOException if there is an error loading the chat FXML
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

  /**
   * Gets the FXML file path for a given participant ID.
   * 
   * @param participantId the ID of the participant
   * @return the FXML file path for the participant, or null if not found
   */
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
   * Gets the trial room scene for returning from flashback.
   * 
   * @return the trial room scene
   */
  public static Scene getTrialRoomScene() {
    return trialRoomScene;
  }

  /**
   * Checks if all chatboxes have been interacted with.
   * 
   * @return true if all three chatboxes have been interacted with, false otherwise
   */
  public static boolean areAllChatboxesInteracted() {
    return chatboxesInteracted.size() >= 3;
  }

  /**
   * Resets all game interactions and state for a new game.
   * Clears chatbox interactions, flashback history, and conversation data.
   */
  public static void resetInteractions() {
    chatboxesInteracted.clear();
    flashbackShown.clear();
    conversationHistories.clear();
    sharedConversationHistory.clear();
    isFirstTime = true;
  }

  // Add this static method to track interactions
  public static void markChatboxInteracted(String participantId) {
    chatboxesInteracted.add(participantId);
    System.out.println("Meaningful interaction with: " + participantId + ". Total: " + chatboxesInteracted.size());
    
    // If we're in the trial room scene, update the button state
    if (trialRoomScene != null && trialRoomScene.getWindow() != null && trialRoomScene.getWindow().isShowing()) {
        for (Node node : trialRoomScene.getRoot().lookupAll("#btnVerdict")) {
            if (node instanceof Button) {
                Button verdictButton = (Button) node;
                boolean allInteracted = chatboxesInteracted.size() >= 3;
                verdictButton.setDisable(!allInteracted);
                if (allInteracted) {
                    verdictButton.setStyle("-fx-background-color: #2ecc71;"); // Green when enabled
                } else {
                    verdictButton.setStyle("-fx-background-color: #95a5a6;"); // Gray when disabled
                }
                break;
            }
        }
    }
}
}
