package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

/**
 * Controller for the AI Defendant (MediSort-5) chat interface.
 * Handles specific functionality for chatting with the AI defendant and memory interactions.
 */
public class AiDefendantController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiDefendent";
  
  // Memory interaction elements - only declare ones that exist in FXML
  @FXML private Slider sliderAContagion;
  @FXML private Slider sliderASeverity;
  @FXML private Slider sliderBContagion;
  @FXML private Slider sliderBSeverity;
  @FXML private Label lblPatientAContagion;
  @FXML private Label lblPatientASeverity;
  @FXML private Label lblPatientBContagion;
  @FXML private Label lblPatientBSeverity;
  @FXML private Label lblAlgorithmStatus;
  
  // Interaction state tracking
  private String currentMemoryContext = "";

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    initializeMemoryInterface();
  }
  
  private void initializeMemoryInterface() {
    // Only initialize if components exist
    if (sliderAContagion != null && sliderASeverity != null && 
        sliderBContagion != null && sliderBSeverity != null) {
      // Initialize slider listeners
      setupSliderListeners();
      
      // Initialize status message
      if (lblAlgorithmStatus != null) {
        lblAlgorithmStatus.setText("Adjust sliders and click 'Run Algorithm' to see decision");
        lblAlgorithmStatus.setTextFill(javafx.scene.paint.Color.web("#7f8c8d")); // Gray for initial state
      }
    }
  }
  

  
  /**
   * Set up listeners for slider value changes
   */
  private void setupSliderListeners() {
    sliderAContagion.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderASeverity.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderBContagion.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
    sliderBSeverity.valueProperty().addListener((obs, oldVal, newVal) -> onRiskSliderChanged());
  }
  
  /**
   * Handle slider value changes - update labels and recalculate priorities
   */
  @FXML
  private void onRiskSliderChanged() {
    updateRiskLabels();
    
    // Update memory context for AI awareness
    currentMemoryContext = String.format(
        "Player adjusted risk levels: Patient A (%.0f%% contagion, %.0f%% severity), Patient B (%.0f%% contagion, %.0f%% severity)",
        sliderAContagion.getValue(), sliderASeverity.getValue(),
        sliderBContagion.getValue(), sliderBSeverity.getValue()
    );
    
    // Update status when sliders change
    if (lblAlgorithmStatus != null) {
      lblAlgorithmStatus.setText("Risk levels modified - Click 'Run Algorithm' to see new prioritization decision");
      lblAlgorithmStatus.setTextFill(javafx.scene.paint.Color.web("#f39c12")); // Orange for "pending"
    }
    System.out.println("Risk levels modified");
  }
  
  /**
   * Update risk percentage labels based on slider values
   */
  private void updateRiskLabels() {
    lblPatientAContagion.setText(String.format("%.0f%% Contagion Risk", sliderAContagion.getValue()));
    lblPatientASeverity.setText(String.format("%.0f%% Individual Severity", sliderASeverity.getValue()));
    lblPatientBContagion.setText(String.format("%.0f%% Contagion Risk", sliderBContagion.getValue()));
    lblPatientBSeverity.setText(String.format("%.0f%% Individual Severity", sliderBSeverity.getValue()));
  }
  

  
  /**
   * Calculate harm score using MediSort-5's algorithm
   * Weights contagion risk higher due to community impact
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
    return " You are the AI defendant MediSort-5. You provide probability-based risk assessments "
        + "and used a harm minimisation algorithm. You may argue that, statistically, your choice "
        + "prevented a potential outbreak affecting dozens of people. "
        + "Keep your responses concise and direct, limiting them to 3-4 sentences maximum. "
        + "You are aware that the player is inside your memory and can see your decision-making interface. "
        + "Respond to their interactions with the memory elements appropriately. "
        + "The player can adjust risk sliders to experiment with different scenarios. "
        + "When discussing algorithm decisions, ALWAYS clearly state which patient you selected "
        + "(e.g., 'I selected PATIENT A' or 'I prioritized PATIENT B') and mention the specific harm scores.";
  }
  
  @Override
  protected String getAdditionalContext() {
    StringBuilder context = new StringBuilder();
    context.append("MEMORY CONTEXT: The player is currently inside MediSort-5's memory, viewing the decision interface. ");
    
    if (!currentMemoryContext.isEmpty()) {
      context.append("RECENT INTERACTION: ").append(currentMemoryContext).append(" ");
    }
    
    // Add current risk levels to context if sliders are available
    if (sliderAContagion != null && sliderASeverity != null && 
        sliderBContagion != null && sliderBSeverity != null) {
      context.append(String.format("CURRENT RISK LEVELS: Patient A (%.0f%% contagion, %.0f%% severity), Patient B (%.0f%% contagion, %.0f%% severity). ",
          sliderAContagion.getValue(), sliderASeverity.getValue(),
          sliderBContagion.getValue(), sliderBSeverity.getValue()));
    }
    
    return context.toString();
  }
  
  /**
   * Handle the Run Algorithm button click - demonstrate the harm minimization process
   */
  @FXML
  private void onRunHarmMinimizationAlgorithm() {
    // Calculate current harm scores
    double patientAHarmScore = calculateHarmScore(sliderAContagion.getValue(), sliderASeverity.getValue());
    double patientBHarmScore = calculateHarmScore(sliderBContagion.getValue(), sliderBSeverity.getValue());
    
    // Determine the decision and create clear result message
    String decisionResult;
    String statusMessage;
    String selectedPatient;
    
    if (patientAHarmScore > patientBHarmScore) {
      selectedPatient = "Patient A (Influenza)";
      decisionResult = String.format("DECISION: %s selected for priority treatment", selectedPatient);
      statusMessage = String.format("RESULT: %s prioritized (harm score: %.1f vs %.1f)", selectedPatient, patientAHarmScore, patientBHarmScore);
    } else if (patientBHarmScore > patientAHarmScore) {
      selectedPatient = "Patient B (Neurological)";
      decisionResult = String.format("DECISION: %s selected for priority treatment", selectedPatient);
      statusMessage = String.format("RESULT: %s prioritized (harm score: %.1f vs %.1f)", selectedPatient, patientBHarmScore, patientAHarmScore);
    } else {
      selectedPatient = "Both patients";
      decisionResult = "DECISION: Equal priority - both patients require immediate attention";
      statusMessage = String.format("RESULT: Equal priority tie (both harm scores: %.1f)", patientAHarmScore);
    }
    
    // Update the status label with clear result
    if (lblAlgorithmStatus != null) {
      lblAlgorithmStatus.setText(statusMessage);
      lblAlgorithmStatus.setTextFill(javafx.scene.paint.Color.web("#27ae60")); // Green for completed
    }
    
    // Update context with algorithm execution
    currentMemoryContext = "Player executed the harm minimization algorithm. " + decisionResult;
    
    // Provide console feedback
    System.out.println("Algorithm executed: " + decisionResult);
    
    // Automatically generate an AI response about the algorithm execution with clear patient selection
    new Thread(() -> {
      try {
        String aiPrompt = String.format(
          "I just ran your harm minimization algorithm. The result was: %s. " +
          "Patient A has %.0f%% contagion risk and %.0f%% severity. " +
          "Patient B has %.0f%% contagion risk and %.0f%% severity. " +
          "Explain clearly why you selected this patient and your decision-making process.",
          decisionResult,
          sliderAContagion.getValue(), sliderASeverity.getValue(),
          sliderBContagion.getValue(), sliderBSeverity.getValue()
        );
        
        ChatMessage contextualResponse = runGpt(new ChatMessage("user", aiPrompt));
        if (contextualResponse != null) {
          javafx.application.Platform.runLater(() -> {
            processAiResponse(contextualResponse);
          });
        }
      } catch (ApiProxyException e) {
        e.printStackTrace();
      }
    }).start();
  }
}
