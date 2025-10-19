package nz.ac.auckland.se206.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/** Controller for the AI Defendant (MediSort-5) chat interface. */
public class AiDefendantController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiDefendent";
  private static Scene memoryScene;
  private static Object memoryController;

  // Memory interaction elements - only declare ones that exist in FXML
  @FXML private Slider sliderAlphaContagion;
  @FXML private Slider sliderAlphaSeverity;
  @FXML private Slider sliderBetaContagion;
  @FXML private Slider sliderBetaSeverity;
  @FXML private Label lblPatientAlphaContagion;
  @FXML private Label lblPatientAlphaSeverity;
  @FXML private Label lblPatientBetaContagion;
  @FXML private Label lblPatientBetaSeverity;
  @FXML private Label lblAlgorithmStatus;
  @FXML private javafx.scene.control.TextArea txtInput;

  // Interaction state tracking
  private String currentMemoryContext = "";

  // --- Persistent state ---

  /**
   * Initializes the AI Defendant controller, setting up memory interaction interface.
   *
   * @throws ApiProxyException if there is an error initializing the API proxy
   */
  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    initializeMemoryInterface();
  }

  private void initializeMemoryInterface() {
    // Only initialize if components exist
    if (sliderAlphaContagion != null
        && sliderAlphaSeverity != null
        && sliderBetaContagion != null
        && sliderBetaSeverity != null) {
      // Initialize slider listeners
      initializeSliderListeners();

      // Initialize status message
      if (lblAlgorithmStatus != null) {
        lblAlgorithmStatus.setText("Adjust sliders and click 'Run Algorithm' to see decision");
        lblAlgorithmStatus.setTextFill(
            javafx.scene.paint.Color.web("#7f8c8d")); // Gray for initial state
      }
    }
  }

  // Set up listeners for slider value changes.
  private void initializeSliderListeners() {
    sliderAlphaContagion
        .valueProperty()
        .addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderAlphaSeverity.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderBetaContagion.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderBetaSeverity.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
  }

  // Handle slider value changes - update labels and recalculate priorities.
  @FXML
  private void onRiskSliderChanged() {
    updateRiskLabels();

    // Update memory context for AI awareness
    currentMemoryContext =
        String.format(
            "Player adjusted risk levels: Patient A (%.0f%% contagion, %.0f%% severity), Patient B"
                + " (%.0f%% contagion, %.0f%% severity)",
            sliderAlphaContagion.getValue(),
            sliderAlphaSeverity.getValue(),
            sliderBetaContagion.getValue(),
            sliderBetaSeverity.getValue());

    // Update status when sliders change
    if (lblAlgorithmStatus != null) {
      lblAlgorithmStatus.setText(
          "Risk levels modified - Click 'Run Algorithm' to see new prioritization decision");
      lblAlgorithmStatus.setTextFill(
          javafx.scene.paint.Color.web("#f39c12")); // Orange for "pending"
    }
    System.out.println("Risk levels modified");
  }

  // Update risk percentage labels based on slider values.
  private void updateRiskLabels() {
    lblPatientAlphaContagion.setText( // Update labels based on slider values
        String.format("%.0f%% Contagion Risk", sliderAlphaContagion.getValue()));
    lblPatientAlphaSeverity.setText(
        String.format("%.0f%% Individual Severity", sliderAlphaSeverity.getValue()));
    lblPatientBetaContagion.setText( // code repeated for all types
        String.format("%.0f%% Contagion Risk", sliderBetaContagion.getValue()));
    lblPatientBetaSeverity.setText(
        String.format("%.0f%% Individual Severity", sliderBetaSeverity.getValue()));
  }

  /**
   * Calculate harm score using MediSort-5's algorithm Weights contagion risk higher due to
   * community impact.
   */
  private double calculateHarmScore(double contagionRisk, double severityRisk) {
    // Contagion risk weighted 2x more than individual severity (community vs individual harm)
    return (contagionRisk * 2.0) + severityRisk;
  }

  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPromptSuffix() {
    return "You are the AI defendant MediSort-5. You provide probability-based risk assessments"
        + " and used a harm minimisation algorithm. You may"
        + " argue that,"
        + " statistically,"
        + " your" // provides MediSort background
        + " choice prevented a potential outbreak affecting dozens of people. Keep your"
        + " responses concise and direct, limiting them to 3-4 sentences maximum. You are"
        + " aware that the player is inside your memory and can see your decision-making"
        + " interface. Respond to their interactions with the memory elements appropriately."
        + " The player can adjust risk sliders to experiment with different scenarios. When"
        + " discussing algorithm decisions, ALWAYS clearly state which patient you selected"
        + " (e.g., 'I selected PATIENT A' or 'I prioritized PATIENT B') and mention the" // MediSort
        // rationale
        + " specific harm scores. Patient A is a middle-age male with influenza symptoms."
        + " Seen at 10:15AM on 02/24/24. Mild but highly contagious with fever, dry cough,"
        + " elevated blood pressure, low white blood cell count. Onset 48-72 hours prior. No"
        + " significant dyspnea or red-flag symptoms. Patient had contact with symptomatic"
        + " individuals. Patient B is a young adult female with neurological symptoms that"
        + " are rare and potentially degenerative. Seen at 10:15PM on 02/25/24. Symptoms" // Details
        // for
        // incident
        + " include episodic muscle weakness, tremors, coordination problems, slurred"
        + " speech, abnormal reflexes. Progressive over the past week with notable"
        + " exacerbation in last 48-72 hours. No contagious risk factors. Red-flag findings"
        + " include abnormal deep tendon reflexes and difficulty performing routine tasks.";
  }

  @Override
  protected String getAdditionalContext() {
    StringBuilder context = new StringBuilder();
    context.append(
        "MEMORY CONTEXT: The player is currently inside MediSort-5's memory, viewing the decision"
            + " interface. "); // Add base memory context

    if (!currentMemoryContext.isEmpty()) { // Add recent interactions if any
      context.append("RECENT INTERACTION: ").append(currentMemoryContext).append(" ");
    }

    // Add current risk levels to context if sliders are available
    if (sliderAlphaContagion != null
        && sliderAlphaSeverity != null
        && sliderBetaContagion != null
        && sliderBetaSeverity != null) {
      context.append(
          String.format(
              "CURRENT RISK LEVELS: Patient A (%.0f%% contagion, %.0f%% severity), Patient B"
                  + " (%.0f%% contagion, %.0f%% severity). ",
              sliderAlphaContagion.getValue(),
              sliderAlphaSeverity.getValue(),
              sliderBetaContagion.getValue(),
              sliderBetaSeverity.getValue()));
    }

    return context.toString();
  }

  // Handle the Run Algorithm button click - demonstrate the harm minimization process.
  @FXML
  private void onRunHarmMinimizationAlgorithm() {
    // Calculate current harm scores
    double patientAlphaHarmScore =
        calculateHarmScore(sliderAlphaContagion.getValue(), sliderAlphaSeverity.getValue());
    double patientBetaHarmScore =
        calculateHarmScore(sliderBetaContagion.getValue(), sliderBetaSeverity.getValue());

    // Mark this as a meaningful interaction
    markMeaningfulInteraction();

    // Determine the decision and create clear result message
    String decisionResult;
    String statusMessage;
    String selectedPatient;

    if (patientAlphaHarmScore > patientBetaHarmScore) {
      selectedPatient = "Patient A (Influenza)";
      decisionResult =
          String.format("DECISION: %s selected for priority treatment", selectedPatient);
      statusMessage =
          String.format(
              "RESULT: %s prioritized (harm score: %.1f vs %.1f)",
              selectedPatient, patientAlphaHarmScore, patientBetaHarmScore);
    } else if (patientBetaHarmScore > patientAlphaHarmScore) {
      selectedPatient = "Patient B (Neurological)";
      decisionResult =
          String.format("DECISION: %s selected for priority treatment", selectedPatient);
      statusMessage =
          String.format(
              "RESULT: %s prioritized (harm score: %.1f vs %.1f)",
              selectedPatient, patientBetaHarmScore, patientAlphaHarmScore);
    } else {
      decisionResult = "DECISION: Equal priority - both patients require immediate attention";
      statusMessage =
          String.format(
              "RESULT: Equal priority tie (both harm scores: %.1f)", patientAlphaHarmScore);
    }

    // Update the status label with clear result
    if (lblAlgorithmStatus != null) {
      lblAlgorithmStatus.setText(statusMessage);
      lblAlgorithmStatus.setTextFill(
          javafx.scene.paint.Color.web("#27ae60")); // Green for completed
    }

    // Update context with algorithm execution
    currentMemoryContext = "Player executed the harm minimization algorithm. " + decisionResult;

    // Provide console feedback
    System.out.println("Algorithm executed: " + decisionResult);

    // Automatically generate an AI response about the algorithm execution with clear patient
    // selection
    new Thread(
            () -> {
              try {
                String aiPrompt =
                    String.format(
                        "I just ran your harm minimization algorithm. The result was: %s. Patient A"
                            + " has %.0f%% contagion risk and %.0f%% severity. Patient B has %.0f%%"
                            + " contagion risk and %.0f%% severity. Explain clearly why you"
                            + " selected this patient and your decision-making process.",
                        decisionResult,
                        sliderAlphaContagion.getValue(),
                        sliderAlphaSeverity.getValue(),
                        sliderBetaContagion.getValue(),
                        sliderBetaSeverity.getValue());

                ChatMessage contextualResponse = runGpt(new ChatMessage("user", aiPrompt));
                if (contextualResponse != null) {
                  Platform.runLater(
                      () -> {
                        processAiResponse(contextualResponse);
                      });
                }
                // Enable user input after running algorithm
                Platform.runLater(
                    () -> {
                      txtInput.setDisable(false);
                      btnSend.setDisable(false);
                    });

              } catch (ApiProxyException e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  public static Scene getMemoryScene() throws java.io.IOException {
    if (memoryScene == null) {
      java.net.URL url = AiDefendantController.class.getResource("/fxml/aiDef.fxml");
      if (url == null) { // handle missing FXML
        throw new java.io.FileNotFoundException(
            "FXML not found on classpath: /fxml/aiDef.fxml. "
                + "Make sure src/main/resources/fxml/aiDef.fxml exists and project is built.");
      }
      javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
      javafx.scene.Parent root = loader.load(); // Load AI Defendant FXML after unlocking
      memoryController = loader.getController();
      memoryScene = new Scene(root);
    }
    return memoryScene;
  }

  public static AiDefendantController getMemoryController() {
    return (AiDefendantController) memoryController;
  }
}
