package nz.ac.auckland.se206;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

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
  private static final int VERDICT_SECONDS = 10;

  private int timeLeft;
  private Runnable onRoundEnd;
  private Runnable onVerdictEnd;
  private final StringProperty timerText = new SimpleStringProperty();
  private Timeline timeline;
  private boolean inVerdictPhase = false;

  private GameTimer() {}

  // === Public methods ===
  public void start(Runnable onRoundEnd, Runnable onVerdictEnd) {
    this.onRoundEnd = onRoundEnd;
    this.onVerdictEnd = onVerdictEnd;
    inVerdictPhase = false;
    timeLeft = ROUND_SECONDS;
    updateTimerText();
    if (timeline != null) {
      timeline.stop();
    }
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();
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

  public StringProperty timerTextProperty() {
    return timerText;
  }

  // === Private methods ===
  private void tick() {
    timeLeft--;
    updateTimerText();
    if (timeLeft <= 0) {
      if (!inVerdictPhase) {
        inVerdictPhase = true;
        timeLeft = VERDICT_SECONDS;
        updateTimerText();
        if (onRoundEnd != null) {
          Platform.runLater(onRoundEnd);
        }
      } else {
        timeline.stop();
        if (onVerdictEnd != null) {
          Platform.runLater(onVerdictEnd);
        }
      }
    }
  }

  private void updateTimerText() {
    String label = inVerdictPhase ? "Final Verdict: " : "Time Left: ";
      int minutes = timeLeft / 60;
      int seconds = timeLeft % 60;

      // Format seconds as two digits (e.g. 5:07 instead of 5:7)
      String timeFormatted = String.format("%d:%02d", minutes, seconds);

      timerText.set(label + timeFormatted);
  }
}
