package nz.ac.auckland.se206.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import nz.ac.auckland.se206.GameTimer;

public class GameOverController {

  @FXML private Button btnReplay;

  /**
   * Handles the replay game button click event, resetting game state and returning to trial room.
   */
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
