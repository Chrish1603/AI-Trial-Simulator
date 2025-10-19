package nz.ac.auckland.se206;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import nz.ac.auckland.se206.controllers.TrialRoomController;
import nz.ac.auckland.se206.controllers.VerdictController;

/**
 * Singleton timer manager for the game. Handles the main round timer and the final verdict timer.
 */
public class GameTimer {
  private static GameTimer instance;

  public static GameTimer getInstance() {
    if (instance == null) {
      instance = new GameTimer();
    }
    return instance;
  }

  private static final int ROUND_SECONDS = 300;
  private static final int VERDICT_SECONDS = 60;

  private Object activeController;
  private int timeLeft;
  private Runnable onRoundEnd;
  private Runnable onVerdictEnd;
  private final StringProperty timerText = new SimpleStringProperty();
  private Timeline timeline;
  private boolean inVerdictPhase = false;
  private Stage currentStage;
  private Runnable timeExpiredCallback;

  private GameTimer() {}

  public void start(Runnable onRoundEnd, Runnable onVerdictEnd) {
    this.onRoundEnd = onRoundEnd; // Set the round end callback
    this.onVerdictEnd = onVerdictEnd;
    inVerdictPhase = false;
    timeLeft = ROUND_SECONDS;
    updateTimerText(); // Ensure timer text is initialized
    if (timeline != null) {
      timeline.stop();
    }
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick())); // Tick every second
    timeline.setCycleCount(Timeline.INDEFINITE); 
    timeline.play();
  }

  public void setCurrentStage(Stage stage) {
    this.currentStage = stage;
  }

  /**
   * Switches directly to the verdict phase with a 60-second timer. Used when navigating directly to
   * the verdict screen.
   */
  public void switchToVerdictPhase() {
    // Only switch if not already in verdict phase
    if (!inVerdictPhase) {
      inVerdictPhase = true;
      timeLeft = VERDICT_SECONDS;
      updateTimerText();

      // If timer is not running, start it
      if (timeline == null || timeline.getStatus() != Timeline.Status.RUNNING) {
        if (timeline != null) {
          timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
      }
    }
  }

  public void stop() {
    if (timeline != null) {
      timeline.stop();
    }
  }

  public boolean isRunning() {
    return timeline != null && timeline.getStatus() == Timeline.Status.RUNNING;
  }

  public boolean isInVerdictPhase() {
    return inVerdictPhase;
  }

public StringProperty getTimerTextProperty() {
  return timerText;
}
public static void setActiveController(Object controller) {
  getInstance().activeController = controller;
}

  /**
   * Sets a callback to run when timer expires
   */
  public void setTimeExpiredCallback(Runnable callback) {
      this.timeExpiredCallback = callback;
  }

  // === Private methods ===
  private void tick() {
    timeLeft--;
    updateTimerText();
    if (timeLeft <= 0) { 
      if (!inVerdictPhase) { // Transition to verdict phase
        inVerdictPhase = true;
        timeLeft = VERDICT_SECONDS;
        updateTimerText();
        handleRoundEnd();
      } else { // End of verdict phase
        timeline.stop();
        handleVerdictEnd();
      }
    }
  }

  /**
   * Updates the timer display
   */
  private void updateTimerText() {
    String label = "Time Left: ";
    int minutes = timeLeft / 60;
    int seconds = timeLeft % 60;

    // Format seconds as two digits (e.g. 5:07 instead of 5:7)
    String timeFormatted = String.format("%d:%02d", minutes, seconds);

    timerText.set(label + timeFormatted);

    if (timeLeft <= 0) {
        // Timer expired
        timeline.stop();
        timerText.set("Time's Up!");
        
        // Execute callback if set
        if (timeExpiredCallback != null) {
            timeExpiredCallback.run();
        }
        
        // ...other timer expiration code...
    }
  }

  private void handleRoundEnd() {
    Platform.runLater(
        () -> {
          System.out.println("Round timer ended!");

          // Check if the current scene is a flashback by examining the controller of the current stage
          boolean isInFlashback = false;
          if (currentStage != null && currentStage.getScene() != null && 
              currentStage.getScene().getUserData() instanceof String) {
            String sceneType = (String) currentStage.getScene().getUserData();
            isInFlashback = "flashback".equals(sceneType);
          }
          
          // Always go to game over if in flashback
          if (isInFlashback) {
            System.out.println("Timer expired while in flashback, showing game over");
            transitionToGameOver();
            return;
          }

          boolean allInteracted = TrialRoomController.areAllChatboxesInteracted();
          System.out.println("All chatboxes interacted: " + allInteracted);
          
          // Check if all characters have been interacted with
          if (allInteracted) {
              // All three chatboxes interacted with - proceed to verdict
              System.out.println("All chatboxes interacted, transitioning to verdict scene");
              transitionToVerdict();
          } else {
              // Not all chatboxes interacted with - game over
              System.out.println("Not all chatboxes interacted, showing game over");
              transitionToGameOver();
          }
        });

    // Still call the onRoundEnd callback for any other processing
    if (onRoundEnd != null) {
      Platform.runLater(onRoundEnd);
    }
  }

private void handleVerdictEnd() {
  Platform.runLater(() -> {
    System.out.println("Verdict timer ended!");
    if (onVerdictEnd != null) {
      onVerdictEnd.run();
    }

    // Try to auto-submit verdict if user ran out of time
    if (activeController instanceof VerdictController) {
        ((VerdictController) activeController).autoSubmitVerdict();
    }

  });
}


  public void transitionToVerdict() {
    Platform.runLater(
        () -> {
          try {
            if (currentStage == null) {
              currentStage = getActiveStage(); // Try to find active stage if not set
            }

            if (currentStage != null) {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verdict.fxml"));
              Parent root = loader.load();
              Scene scene = new Scene(root); // Set up verdict scene
              scene.getProperties().put("controller", loader.getController());
              currentStage.setScene(scene);
              currentStage.show();

              VerdictController controller = loader.getController();
              controller.startVerdictTimer();
            } else {
              System.err.println("Error: Could not get a stage for scene transition");
            }
          } catch (IOException e) {
            System.err.println("Error loading verdict scene: " + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  public void transitionToGameOver() {
    Platform.runLater(
        () -> {
          try {
            if (currentStage == null) {
              currentStage = getActiveStage(); // Try to get the active stage if not set
            }

            if (currentStage != null) {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameover.fxml"));
              Parent root = loader.load(); // Load game over scene
              Scene scene = new Scene(root); 
              currentStage.setScene(scene);
              currentStage.show();
            } else {
              System.err.println("Error: Could not get a stage for scene transition");
            }
          } catch (IOException e) {
            System.err.println("Error loading game over scene: " + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  private Stage getActiveStage() {
    for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
      if (window instanceof Stage && window.isShowing()) {
        return (Stage) window;
      }
    }
    return null;
  }
}
