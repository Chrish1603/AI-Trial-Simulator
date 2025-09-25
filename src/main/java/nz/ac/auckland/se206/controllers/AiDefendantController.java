package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the AI Defendant (MediSort-5) chat interface.
 * Handles specific functionality for chatting with the AI defendant and memory interactions.
 */
public class AiDefendantController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiDefendent";
  
  // Memory interaction elements
  @FXML private AnchorPane memoryPanel;
  @FXML private ProgressBar patientAContagion;
  @FXML private ProgressBar patientASeverity;
  @FXML private ProgressBar patientBContagion;
  @FXML private ProgressBar patientBSeverity;
  @FXML private Label lblPatientAContagion;
  @FXML private Label lblPatientASeverity;
  @FXML private Label lblPatientBContagion;
  @FXML private Label lblPatientBSeverity;
  @FXML private Rectangle decisionMatrixA;
  @FXML private Rectangle decisionMatrixB;
  @FXML private Button btnRunAlgorithm;
  @FXML private Label lblAlgorithmStatus;
  
  // Interaction state tracking
  private boolean hasExaminedPatientA = false;
  private boolean hasExaminedPatientB = false;
  private boolean hasRunAlgorithm = false;
  private String currentMemoryContext = "";

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    initializeMemoryInterface();
  }
  
  private void initializeMemoryInterface() {
    // Set initial algorithm status
    lblAlgorithmStatus.setText("Algorithm Status: Ready - Click elements to explore my decision process");
    
    // Add hover effects for interactive elements
    setupHoverEffects();
  }
  
  private void setupHoverEffects() {
    // Add hover effects for decision matrix rectangles
    decisionMatrixA.setOnMouseEntered(e -> decisionMatrixA.setOpacity(0.8));
    decisionMatrixA.setOnMouseExited(e -> decisionMatrixA.setOpacity(1.0));
    
    decisionMatrixB.setOnMouseEntered(e -> decisionMatrixB.setOpacity(0.8));
    decisionMatrixB.setOnMouseExited(e -> decisionMatrixB.setOpacity(1.0));
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
        + "Respond to their interactions with the memory elements appropriately.";
  }
  
  @Override
  protected String getAdditionalContext() {
    StringBuilder context = new StringBuilder();
    context.append("MEMORY CONTEXT: The player is currently inside MediSort-5's memory, viewing the decision interface. ");
    
    if (hasExaminedPatientA) {
      context.append("Player has examined Patient A's risk profile (85% contagion, 35% individual severity). ");
    }
    
    if (hasExaminedPatientB) {
      context.append("Player has examined Patient B's risk profile (0% contagion, 90% individual severity). ");
    }
    
    if (hasRunAlgorithm) {
      context.append("Player has run the harm minimization algorithm and seen the decision process. ");
    }
    
    if (!currentMemoryContext.isEmpty()) {
      context.append("RECENT INTERACTION: ").append(currentMemoryContext).append(" ");
    }
    
    return context.toString();
  }
  
  /**
   * Handle examining Patient A's profile
   */
  @FXML
  private void onExaminePatientA() {
    hasExaminedPatientA = true;
    currentMemoryContext = "Player examined Patient A's profile, seeing high contagion risk but moderate individual severity.";
    
    // Visual feedback
    decisionMatrixA.setStroke(javafx.scene.paint.Color.YELLOW);
    decisionMatrixA.setStrokeWidth(3.0);
    lblAlgorithmStatus.setText("Examining Patient A: High community risk, moderate individual risk");
    
    // Automatically trigger AI response about this interaction
    appendChatMessage(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("system", 
        "Player is examining Patient A's risk assessment in your memory interface."));
    
    // Generate contextual AI response
    new Thread(() -> {
      try {
        nz.ac.auckland.apiproxy.chat.openai.ChatMessage contextualResponse = 
            runGpt(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("user", 
                "I'm looking at Patient A's risk profile in your memory. Explain this assessment."));
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
  
  /**
   * Handle examining Patient B's profile
   */
  @FXML
  private void onExaminePatientB() {
    hasExaminedPatientB = true;
    currentMemoryContext = "Player examined Patient B's profile, noting zero contagion risk but very high individual severity.";
    
    // Visual feedback
    decisionMatrixB.setStroke(javafx.scene.paint.Color.YELLOW);
    decisionMatrixB.setStrokeWidth(3.0);
    lblAlgorithmStatus.setText("Examining Patient B: No community risk, critical individual risk");
    
    // Automatically trigger AI response about this interaction
    appendChatMessage(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("system", 
        "Player is examining Patient B's risk assessment in your memory interface."));
    
    // Generate contextual AI response
    new Thread(() -> {
      try {
        nz.ac.auckland.apiproxy.chat.openai.ChatMessage contextualResponse = 
            runGpt(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("user", 
                "I'm looking at Patient B's risk profile in your memory. Explain this assessment."));
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
  
  /**
   * Handle running the harm minimization algorithm
   */
  @FXML
  private void onRunHarmMinimizationAlgorithm() {
    hasRunAlgorithm = true;
    currentMemoryContext = "Player activated the harm minimization algorithm, witnessing the decision-making process that prioritized Patient A.";
    
    // Visual feedback - animate the algorithm execution
    btnRunAlgorithm.setText("Running...");
    btnRunAlgorithm.setDisable(true);
    lblAlgorithmStatus.setText("ALGORITHM EXECUTING: Calculating optimal harm reduction...");
    
    // Simulate algorithm execution with visual updates
    javafx.animation.Timeline timeline = new javafx.animation.Timeline(
        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
          lblAlgorithmStatus.setText("RESULT: Patient A prioritized - minimizes community harm");
          btnRunAlgorithm.setText("Algorithm Complete");
          btnRunAlgorithm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        })
    );
    timeline.play();
    
    // Automatically trigger AI response about running the algorithm
    appendChatMessage(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("system", 
        "Player has executed the harm minimization algorithm in your memory."));
    
    // Generate contextual AI response
    new Thread(() -> {
      try {
        nz.ac.auckland.apiproxy.chat.openai.ChatMessage contextualResponse = 
            runGpt(new nz.ac.auckland.apiproxy.chat.openai.ChatMessage("user", 
                "I just ran your harm minimization algorithm. Walk me through your decision logic."));
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