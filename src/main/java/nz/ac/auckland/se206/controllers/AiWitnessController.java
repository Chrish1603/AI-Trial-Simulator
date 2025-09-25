package nz.ac.auckland.se206.controllers;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the AI Witness (PathoScan-7) chat interface.
 * Handles specific functionality for chatting with the AI witness.
 */
public class AiWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiWitness";

  @FXML private ImageView imgHandScanner;
  @FXML private ProgressBar progressScan;
  @FXML private Label lblScanStatus;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;

  private Timeline scanTimeline;
  private double scanProgress = 0.0;
  private static final double SCAN_DURATION = 2.0; // seconds to unlock
  private static final String SCAN_SUCCESS_IMAGE = "/images/scan_success.png";
  private static final String SCAN_FAIL_IMAGE = "/images/scan_fail.png";
  private static final String SCAN_DEFAULT_IMAGE = "/images/handscanner.png";

  private static AiWitnessController memoryController;
  private static javafx.scene.Scene memoryScene;

  // Add static flag for scanner state
  private static boolean isUnlocked = false;

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    System.out.println("imgHandScanner: " + imgHandScanner);
    System.out.println("progressScan: " + progressScan);
    System.out.println("lblScanStatus: " + lblScanStatus);
    if (isUnlocked) {
      // Already unlocked, restore success state
      lblScanStatus.setText("Authentication Successful.\nWelcome, Investigator.");
      imgHandScanner.setEffect(null);
      progressScan.setProgress(1.0);
      txtInput.setDisable(false);
      btnSend.setDisable(false);
      imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_SUCCESS_IMAGE)));
      imgHandScanner.setOnMousePressed(null);
      imgHandScanner.setOnMouseReleased(null);
    } else {
      setupHandScanner();
      txtInput.setDisable(true);
      btnSend.setDisable(true);
    }
  }

  private void setupHandScanner() {
    progressScan.setProgress(0.0);
    lblScanStatus.setText("Hold to Authenticate");

    imgHandScanner.setOnMousePressed(this::onScanStart);
    imgHandScanner.setOnMouseReleased(this::onScanEnd);
  }

  private void onScanStart(MouseEvent event) {
    System.out.println("Scan started");
    scanProgress = 0.0;
    progressScan.setProgress(0.0);
    lblScanStatus.setText("Scanning...");
    imgHandScanner.setEffect(new Glow(0.7));
    imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_DEFAULT_IMAGE)));

    scanTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
      scanProgress += 0.05 / SCAN_DURATION;
      progressScan.setProgress(scanProgress);
      // Animate glow pulsing
      double glowLevel = 0.7 + 0.3 * Math.sin(scanProgress * Math.PI * 4);
      imgHandScanner.setEffect(new Glow(glowLevel));
      if (scanProgress >= 1.0) {
        scanTimeline.stop();
        onScanComplete();
      }
    }));
    scanTimeline.setCycleCount(Timeline.INDEFINITE);
    scanTimeline.play();
  }

  private void onScanEnd(MouseEvent event) {
    System.out.println("Scan ended");
    if (scanProgress < 1.0) {
      // Released too soon
      if (scanTimeline != null) scanTimeline.stop();
      progressScan.setProgress(0.0);
      lblScanStatus.setText("Scan Incomplete. Please retry.");
      imgHandScanner.setEffect(null);
      imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_FAIL_IMAGE)));
    } else {
      // If scan was successful, do nothing here (handled in onScanComplete)
    }
  }

  private void onScanComplete() {
    isUnlocked = true; // Set static flag
    lblScanStatus.setText("Authentication Successful.\nWelcome, Investigator.");
    imgHandScanner.setEffect(null);
    progressScan.setProgress(1.0);
    txtInput.setDisable(false);
    btnSend.setDisable(false);
    imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_SUCCESS_IMAGE)));
    imgHandScanner.setOnMousePressed(null);
    imgHandScanner.setOnMouseReleased(null);
    txtaChat.appendText("PathoScan-7: Simulations showed Patient A had a 73% chance of causing a facility outbreak within 48 hours, infecting 15–25 others and risking up to six deaths. By deprioritizing treatment, MediSort-5 cut outbreak risk to under 5%, statistically saving more lives overall.\n\n");
    imgGraph.setImage(new Image(getClass().getResourceAsStream("/images/ai-witness-graph.png")));
    imgGraph.setVisible(true);
    // Disable further scanning
    imgHandScanner.setOnMousePressed(null);
    imgHandScanner.setOnMouseReleased(null);
  }

  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPromptSuffix() {
    return " You are the AI witness PathoScan-7, a Disease Spread AI. "
        + "You are an independent system that models contagion spread in the city. "
        + "You will testify that Patient A's illness had a very high transmission potential "
        + "in the care facility. You argue that MediSort-5's decision statistically protected more lives. "
        + "You speak in a precise, analytical manner with statistical data to support your testimony.";
  }
  
  @Override
  protected String getAdditionalContext() {
    return getStatisticalContext();
  }
  
  /**
   * Provides statistical analysis context specific to PathoScan-7's testimony.
   * This method can be used to generate context-specific responses.
   * 
   * @return statistical context for disease spread analysis
   */
  public String getStatisticalContext() {
    return "Current statistical analysis: Patient A's condition had a 73% probability of causing "
        + "a facility-wide outbreak affecting 15-25 individuals within 48 hours. "
        + "MediSort-5's prioritization algorithm prevented this scenario with 94.7% confidence.";
  }

  public static javafx.scene.Scene getMemoryScene() throws IOException {
    if (memoryScene == null) {
      javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
        AiWitnessController.class.getResource("/fxml/aiWit.fxml"));
      javafx.scene.Parent root = loader.load();
      memoryController = loader.getController();
      memoryScene = new javafx.scene.Scene(root);
    }
    return memoryScene;
  }

  public static AiWitnessController getMemoryController() throws IOException {
    if (memoryController == null) {
      getMemoryScene();
    }
    return memoryController;
  }
}