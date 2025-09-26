package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Base controller for all chat interfaces. Contains common functionality for chatting with AI
 * characters and managing conversation histories.
 */
public class ChatController {
  protected static Scene previousScene;
  protected static ChatController instance;

  // Reference to per-participant conversation histories in TrialRoomController
  protected static Map<String, java.util.List<String>> conversationHistories =
      nz.ac.auckland.se206.controllers.TrialRoomController.conversationHistories;
  // Reference to shared conversation history (excluding flashbacks)
  protected static java.util.List<String> sharedConversationHistory =
      nz.ac.auckland.se206.controllers.TrialRoomController.sharedConversationHistory;

  @FXML protected ImageView imgDefendant;
  @FXML protected javafx.scene.control.Label lblTimer;
  @FXML protected TextArea txtaChat;
  @FXML protected TextField txtInput;
  @FXML protected Button btnSend;
  @FXML protected Button btnBack;

  protected static final Map<String, String> DISPLAY_NAME_MAP =
      Map.of(
          "aiDefendent", "MediSort-5",
          "humanWitness", "Dr. Payne Gaun",
          "aiWitness", "PathoScan-7");

  // === Instance fields ===
  protected String participantRole;
  protected ChatCompletionRequest chatCompletionRequest;

  private Timeline loadingTimeline;
  private int loadingDotCount = 1;
  private String loadingBaseText;

  // === Methods that can be overridden by subclasses ===
  protected String getParticipantRole() {
    // Use the dynamically set participant role if available, otherwise use default
    return participantRole != null ? participantRole : "genericChat";
  }

  protected String getSystemPromptSuffix() {
    // Default system prompt suffix - subclasses should override this
    return " You are a helpful assistant in the trial room. Keep your responses concise and to the"
               + " point, limiting them to 3-4 sentences maximum.";
  }

  /**
   * Provides additional context that can be added to the system prompt. Subclasses can override
   * this to provide dynamic context based on UI state.
   *
   * @return additional context string, or empty string if no additional context
   */
  protected String getAdditionalContext() {
    return "";
  }

  // === Static utility methods ===
  public static void setPreviousScene(Scene scene) {
    previousScene = scene;
  }

  public static void clearChat() {
    if (instance != null && instance.txtaChat != null) {
      instance.txtaChat.clear();
    }
  }

  public static void showConversationHistory(java.util.List<String> history) {
    if (instance != null && instance.txtaChat != null && history != null) {
      instance.txtaChat.clear();
      for (String msg : history) {
        instance.txtaChat.appendText(msg + "\n\n");
      }
    }
  }

  public static void appendSystemMessage(String message) {
    if (instance != null && instance.txtaChat != null) {
      instance.txtaChat.appendText("SYSTEM: " + message + "\n\n");
    }
  }

  public static void appendParticipantMessage(String role, String message) {
    if (instance != null && instance.txtaChat != null) {
      instance.txtaChat.appendText(instance.getDisplayName(role) + ": " + message + "\n\n");
    }
  }

  // === FXML lifecycle ===
  @FXML
  public void initialize() throws ApiProxyException {
    // Set the static instance for external access
    instance = this;
    // Set the participant role for this controller
    this.participantRole = getParticipantRole();
    // Initialize chat request
    initializeChatRequest();
    // Bind timer label to global timer
    if (lblTimer != null) {
      lblTimer
          .textProperty()
          .bind(nz.ac.auckland.se206.GameTimer.getInstance().timerTextProperty());

      // Store current stage for timer transitions
      javafx.application.Platform.runLater(
          () -> {
            if (lblTimer != null
                && lblTimer.getScene() != null
                && lblTimer.getScene().getWindow() instanceof javafx.stage.Stage) {
              nz.ac.auckland.se206.GameTimer.getInstance()
                  .setCurrentStage((javafx.stage.Stage) lblTimer.getScene().getWindow());
            }
          });
    }
    if (txtInput != null) {
      txtInput.setOnAction(
          event -> {
            try {
              onSendMessage(null); // null is fine since ActionEvent is not used
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }
  }

  // === Public instance methods ===
  public void setParticipant(String participantId) {
    this.participantRole = participantId;
  }

  /**
   * Initializes the chat completion request with default settings. This method is called
   * automatically during initialization.
   */
  protected void initializeChatRequest() {
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
              .setMaxTokens(150); // Limited to 150 tokens for concise responses
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  // === Event handler methods ===
  @FXML
  protected void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();

    // Ensure chatCompletionRequest is initialized
    if (chatCompletionRequest == null) {
      initializeChatRequest();
    }

    // Process user message
    processUserMessage(message);

    // Generate AI response in background thread
    generateAiResponse(new ChatMessage("user", message));
  }

  /** Processes and displays the user's message, adding it to conversation histories. */
  private void processUserMessage(String message) {
    // Display user message
    txtaChat.appendText("User: " + message + "\n\n");

    // Add to conversation histories
    String userMessage = "User: " + message;
    conversationHistories
        .computeIfAbsent(participantRole, k -> new java.util.ArrayList<>())
        .add(userMessage);
    sharedConversationHistory.add(userMessage);
  }

  /** Generates AI response in a background thread and updates the UI. */
  private void generateAiResponse(ChatMessage userMessage) {

    javafx.application.Platform.runLater(
        () -> {
          loadingBaseText = getDisplayName(participantRole) + ": Loading";
          txtaChat.appendText(loadingBaseText + " .\n\n");
          txtInput.setDisable(true);
          btnSend.setDisable(true);

          // Start loading animation
          startLoadingAnimation();
        });

    new Thread(
            () -> {
              try {
                ChatMessage aiResponse = runGpt(userMessage);
                if (aiResponse != null) {
                  javafx.application.Platform.runLater(
                      () -> {
                        stopLoadingAnimation();
                        removeLoadingText();
                        processAiResponse(aiResponse);
                        txtInput.setDisable(false);
                        btnSend.setDisable(false);
                      });
                } else {
                  javafx.application.Platform.runLater(
                      () -> {
                        stopLoadingAnimation();
                        removeLoadingText();
                        txtaChat.appendText(
                            "SYSTEM: No response received from AI. Please try again.\n\n");
                        txtInput.setDisable(false);
                        btnSend.setDisable(false);
                      });
                }
              } catch (ApiProxyException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(
                    () -> {
                      stopLoadingAnimation();
                      removeLoadingText();
                      txtaChat.appendText(
                          "SYSTEM: Error generating response. Please try again.\n\n");
                      txtInput.setDisable(false);
                      btnSend.setDisable(false);
                    });
              }
            })
        .start();
  }

  /** Processes and displays the AI response, adding it to conversation histories. */
  protected void processAiResponse(ChatMessage aiResponse) {
    // Create display response with correct role
    ChatMessage displayResponse = new ChatMessage(participantRole, aiResponse.getContent());
    appendChatMessage(displayResponse);

    // Add to conversation histories
    String responseMessage = getDisplayName(participantRole) + ": " + aiResponse.getContent();
    conversationHistories
        .computeIfAbsent(participantRole, k -> new java.util.ArrayList<>())
        .add(responseMessage);
    sharedConversationHistory.add(responseMessage);
  }

  @FXML
  protected void onGoBack(ActionEvent event) {
    // Return to the previous scene if available, otherwise fallback to reloading
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    if (previousScene != null) {
      stage.setScene(previousScene);
      stage.show();
    } else {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trialroom.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // === Protected helper methods ===
  protected String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    map.put("participant", getDisplayName(participantRole));
    String basePrompt = PromptEngineering.getPrompt("chat.txt", map);
    String suffix = getSystemPromptSuffix();
    String additionalContext = getAdditionalContext();

    return basePrompt + suffix + (additionalContext.isEmpty() ? "" : " " + additionalContext);
  }

  protected void appendChatMessage(ChatMessage msg) {
    txtaChat.appendText(getDisplayName(msg.getRole()) + ": " + msg.getContent() + "\n\n");
  }

  protected String getDisplayName(final String role) {
    return "user".equals(role) ? "User" : DISPLAY_NAME_MAP.getOrDefault(role, "User");
  }

  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Create a fresh request for each message to include full conversation context
    ChatCompletionRequest freshRequest = createFreshChatRequest();

    // Add system prompt with participant role context
    String systemPrompt = getSystemPrompt();
    freshRequest.addMessage("system", systemPrompt);

    // Add conversation history as context
    addConversationHistoryToRequest(freshRequest);

    // Add the current message
    freshRequest.addMessage(msg);

    try {
      System.out.println("DEBUG: System prompt length: " + systemPrompt.length() + " characters");
      System.out.println("DEBUG: User message: " + msg.getContent());

      ChatCompletionResult chatCompletionResult = freshRequest.execute();

      if (chatCompletionResult == null || chatCompletionResult.getChoices() == null) {
        System.err.println("ERROR: No response or choices returned from API");
        return null;
      }

      // Check if there are any choices
      boolean hasChoices = false;
      for (@SuppressWarnings("unused") Choice choice : chatCompletionResult.getChoices()) {
        hasChoices = true;
        break;
      }

      if (!hasChoices) {
        System.err.println("ERROR: No choices returned from API");
        return null;
      }

      Choice result = chatCompletionResult.getChoices().iterator().next();
      System.out.println("DEBUG: Received response: " + result.getChatMessage().getContent());
      return result.getChatMessage();
    } catch (ApiProxyException e) {
      System.err.println("ERROR: API call failed - " + e.getMessage());
      return null;
    } catch (Exception e) {
      System.err.println("ERROR: Unexpected error - " + e.getMessage());
      return null;
    }
  }

  /** Creates a fresh chat completion request with standard settings. */
  private ChatCompletionRequest createFreshChatRequest() throws ApiProxyException {
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    return new ChatCompletionRequest(config)
        .setN(1)
        .setTemperature(0.2)
        .setTopP(0.5)
        .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
        .setMaxTokens(150); // Limited to 150 tokens for concise responses
  }

  /**
   * Adds conversation history to the chat request for context. Limits history to prevent token
   * overflow.
   */
  private void addConversationHistoryToRequest(ChatCompletionRequest request) {
    // Limit conversation history to last 6 messages to prevent token overflow
    final int MAX_HISTORY_MESSAGES = 6;

    // Add participant's own history (recent messages only)
    java.util.List<String> participantHistory = conversationHistories.get(participantRole);
    if (participantHistory != null) {
      int startIndex = Math.max(0, participantHistory.size() - MAX_HISTORY_MESSAGES);
      java.util.List<String> recentHistory =
          participantHistory.subList(startIndex, participantHistory.size());
      addHistoryMessagesToRequest(request, recentHistory);
    }

    // Add shared conversation history (recent messages only, excluding messages already in
    // participant's history)
    java.util.List<String> currentHistory = conversationHistories.get(participantRole);
    int sharedStartIndex = Math.max(0, sharedConversationHistory.size() - MAX_HISTORY_MESSAGES);

    for (int i = sharedStartIndex; i < sharedConversationHistory.size(); i++) {
      String sharedMsg = sharedConversationHistory.get(i);
      if (currentHistory == null || !currentHistory.contains(sharedMsg)) {
        addParsedMessageToRequest(request, sharedMsg);
      }
    }
  }

  /** Adds a list of history messages to the chat request. */
  private void addHistoryMessagesToRequest(
      ChatCompletionRequest request, java.util.List<String> history) {
    if (history != null) {
      for (String historyMsg : history) {
        addParsedMessageToRequest(request, historyMsg);
      }
    }
  }

  /** Parses a history message and adds it to the chat request. */
  private void addParsedMessageToRequest(ChatCompletionRequest request, String historyMsg) {
    String[] parts = historyMsg.split(": ", 2);
    if (parts.length == 2) {
      String speaker = parts[0];
      String content = parts[1];
      String role = mapSpeakerToRole(speaker);
      request.addMessage(role, content);
    }
  }

  /** Maps display names back to API roles. */
  private String mapSpeakerToRole(String speaker) {
    switch (speaker) {
      case "MediSort-5":
      case "Dr. Payne Gaun":
      case "PathoScan-7":
        return "assistant";
      case "User":
      default:
        return "user";
    }
  }

  private void startLoadingAnimation() {
    loadingDotCount = 1;
    loadingTimeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(0.5),
                event -> {
                  loadingDotCount = (loadingDotCount % 3) + 1;
                  String dots = " " + ".".repeat(loadingDotCount);
                  removeLoadingText();
                  txtaChat.appendText(loadingBaseText + dots + "\n\n");
                }));
    loadingTimeline.setCycleCount(Timeline.INDEFINITE);
    loadingTimeline.play();
  }

  private void stopLoadingAnimation() {
    if (loadingTimeline != null) {
      loadingTimeline.stop();
      loadingTimeline = null;
    }
  }

  private void removeLoadingText() {
    String chatText = txtaChat.getText();
    // Remove any line that starts with the loading base text
    chatText =
        chatText.replaceAll(
            "(?m)^" + java.util.regex.Pattern.quote(loadingBaseText) + ".*\\n\\n", "");
    txtaChat.setText(chatText);
  }
}
