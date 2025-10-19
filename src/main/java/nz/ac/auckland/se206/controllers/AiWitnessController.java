package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

public class AiWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiWitness";

  @FXML private ImageView imgHandScanner;
  @FXML private ProgressBar progressScan;
  @FXML private Label lblScanStatus;
  @FXML private TextArea txtInput;
  @FXML private Button btnSend;
  @FXML private ImageView imgGraph;
  @FXML private Button btnGoBack; // Add this field

  private Timeline scanTimeline;
  private double scanProgress = 0.0;
  private static final double SCAN_DURATION = 2.0; // seconds to unlock
  private static final String SCAN_SUCCESS_IMAGE = "/images/scan_success.png";
  private static final String SCAN_FAIL_IMAGE = "/images/scan_fail.png";
  private static final String SCAN_DEFAULT_IMAGE = "/images/handscanner.png";

  private static AiWitnessController memoryController;
  private static javafx.scene.Scene memoryScene;

  // --- Persistent state ---
  private static boolean isUnlocked = false;
  private static String memoryChatText = "";
  private static Image memoryGraphImage = null;

  /**
   * Initializes the AI Witness controller, setting up the hand scanner interface.
   *
   * @throws ApiProxyException if there is an error initializing the API proxy
   */

  /**
   * Resets the static state variables for the AI Witness controller. This should be called when
   * restarting the game.
   */
  public static void resetState() {
    isUnlocked = false;
    memoryChatText = "";
    memoryGraphImage = null;
    memoryScene = null;
    memoryController = null;
    System.out.println("AI Witness state reset");
  }

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();

    if (isUnlocked) {
      // Restore unlocked state
      lblScanStatus.setText("Authentication Successful.\nWelcome");
      progressScan.setProgress(1.0);
      txtInput.setDisable(false);
      btnSend.setDisable(false);
      imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_SUCCESS_IMAGE)));
      imgHandScanner.setEffect(null);
      imgHandScanner.setOnMousePressed(null);
      imgHandScanner.setOnMouseReleased(null);

      // Restore chat and graph if available
      if (!memoryChatText.isEmpty()) {
        txtaChat.setText(memoryChatText);
      }
      if (memoryGraphImage != null) {
        imgGraph.setImage(memoryGraphImage);
        imgGraph.setVisible(true);
      }
    } else {
      // Setup scanner for first use
      setupHandScanner();
      txtInput.setDisable(true);
      btnSend.setDisable(true);
    }
    txtInput.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case ENTER:
              if (!event.isShiftDown()) {
                event.consume(); // prevent newline
                btnSend.fire(); // simulate send button
              }
              break;
            default:
              break;
          }
        });
  }

  private void setupHandScanner() {
    progressScan.setProgress(0.0);
    lblScanStatus.setText("Hold to Authenticate");

    imgHandScanner.setOnMousePressed(this::onScanStart);
    imgHandScanner.setOnMouseReleased(this::onScanEnd);
  }

  private void onScanStart(MouseEvent event) {
    scanProgress = 0.0;
    progressScan.setProgress(0.0);
    lblScanStatus.setText("Scanning...");
    imgHandScanner.setEffect(new Glow(0.7)); // start glowing for interaction
    imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_DEFAULT_IMAGE)));

    scanTimeline =
        new Timeline(
            new KeyFrame(
                Duration.millis(50),
                e -> {
                  scanProgress += 0.05 / SCAN_DURATION; // increment progress for 50ms frame
                  progressScan.setProgress(scanProgress);
                  imgHandScanner.setEffect(
                      new Glow(0.7 + 0.3 * Math.sin(scanProgress * Math.PI * 4)));
                  if (scanProgress >= 1.0) { // completed scan
                    scanTimeline.stop();
                    onScanComplete();
                  }
                }));
    scanTimeline.setCycleCount(Timeline.INDEFINITE); // repeat until stopped
    scanTimeline.play();
  }

  private void onScanEnd(MouseEvent event) {
    if (scanProgress < 1.0) {
      if (scanTimeline != null) scanTimeline.stop();
      progressScan.setProgress(0.0);
      lblScanStatus.setText("Scan Incomplete. Please retry.");
      imgHandScanner.setEffect(null);
      imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_FAIL_IMAGE)));
    }
  }

  private void onScanComplete() {
    isUnlocked = true;
    lblScanStatus.setText("Authentication Successful.\nWelcome, Investigator.");
    progressScan.setProgress(1.0);
    txtInput.setDisable(false);
    btnSend.setDisable(false);
    imgHandScanner.setImage(new Image(getClass().getResourceAsStream(SCAN_SUCCESS_IMAGE)));
    imgHandScanner.setEffect(null);
    imgHandScanner.setOnMousePressed(null);
    imgHandScanner.setOnMouseReleased(null);

    // Mark this as a meaningful interaction
    markMeaningfulInteraction();

    // Append AI witness text
    String aiText =
        "PathoScan-7: My role is to analyze and provide insights on MediSort-5's patient"
            + " prioritization. According to my calculations, by prioritizing patient A (flu),"
            + " decreases the outbreak risk to under 5%, statistically saving more lives.\n\n";
    txtaChat.appendText(aiText);
    javafx.application.Platform.runLater(() -> txtaChat.setScrollTop(Double.MAX_VALUE));

    conversationHistories
        .computeIfAbsent(participantRole, k -> new java.util.ArrayList<>())
        .add(aiText.trim());
    sharedConversationHistory.add(aiText.trim());

    // Set and show graph
    Image graph = new Image(getClass().getResourceAsStream("/images/ai-witness-graph.png"));
    imgGraph.setImage(graph);
    imgGraph.setVisible(true);

    // Save to persistent memory
    memoryChatText = txtaChat.getText();
    memoryGraphImage = imgGraph.getImage();
  }

  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPrompt() {
    return PromptEngineering.getPrompt("aiWitness.txt", new HashMap<>());
  }

  @Override
  protected String getSystemPromptSuffix() {
    return "You are the AI witness PathoScan-7, a Disease Spread AI. You are an independent system"
               + " that models contagion spread in the city. You will testify that Patient A's"
               + " illness had a very high transmission potential in the"
               + " care facility. You argue" // Ensures clarity for LLM
        + " that MediSort-5's decision statistically protected more lives. You"
        + " speak in a" // supports medisort
        + " precise, analytical manner with statistical data to support your testimony. Keep"
        + " your responses concise and direct, limiting them to 3-4 sentences maximum.";
  }

  @Override
  protected String getAdditionalContext() {
    return getStatisticalContext();
  }

  public String getStatisticalContext() {
    return "Current statistical analysis: Patient A's condition had a 73% probability of causing "
        + "a facility-wide outbreak affecting 15-25 individuals within 48 hours. "
        + "MediSort-5's prioritization algorithm prevented this scenario with 94.7% confidence.";
  }

  public static javafx.scene.Scene getMemoryScene() throws IOException {
    if (memoryScene == null) { // ensure memory is there
      javafx.fxml.FXMLLoader loader = // Load AI Witness FXML after unlocking
          new javafx.fxml.FXMLLoader(AiWitnessController.class.getResource("/fxml/aiWit.fxml"));
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
