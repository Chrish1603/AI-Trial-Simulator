package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

public class ChatController {
  private static Scene previousScene;
  private static ChatController instance;

  // Reference to per-participant conversation histories in TrialRoomController
  private static Map<String, java.util.List<String>> conversationHistories =
      nz.ac.auckland.se206.controllers.TrialRoomController.conversationHistories;

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

  @FXML private ImageView imgDefendant;
  @FXML private ImageView imgGraph;
  @FXML private javafx.scene.control.Label lblTimer;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Button btnBack;

  private static final Map<String, String> DISPLAY_NAME_MAP =
      Map.of(
          "aiDefendent", "MediSort-5",
          "humanWitness", "Dr. Payne Gaun",
          "aiWitness", "PathoScan-7");

  // === Instance fields ===
  private String participantRole;
  private ChatCompletionRequest chatCompletionRequest;

  // === FXML lifecycle ===
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    // Set the static instance for external access
    instance = this;
    // Bind timer label to global timer
    if (lblTimer != null) {
      lblTimer
          .textProperty()
          .bind(nz.ac.auckland.se206.GameTimer.getInstance().timerTextProperty());
    }
  }

  // === Static utility methods ===
  public static void setPreviousScene(Scene scene) {
    previousScene = scene;
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

  // === Public instance methods ===
  public void setParticipant(String participantId) {
    this.participantRole = participantId;
  }

  public void setProfession(String profession) {
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
              .setMaxTokens(100);
      runGpt(new ChatMessage("system", getSystemPrompt()));
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  // === Event handler methods ===
  @FXML
  private void onShowGraph() {
    if (imgGraph != null) {
      imgGraph.setVisible(true);
    }
  }

  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();
    // Ensure chatCompletionRequest is initialized
    if (chatCompletionRequest == null) {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(ChatCompletionRequest.Model.GPT_4_1_NANO)
              .setMaxTokens(100);
    }
    ChatMessage msg = new ChatMessage("user", message);
    // Only print the user's message once, with correct display name
    // Always show 'User' for user's own messages
    txtaChat.appendText("User: " + message + "\n\n");
    // Add user message to this participant's history only
    conversationHistories
        .computeIfAbsent(participantRole, k -> new java.util.ArrayList<>())
        .add("User: " + message);
    // Run GPT in a background thread
    new Thread(
            new Runnable() {
              public void run() {
                ChatMessage aiResponse = null;
                try {
                  aiResponse = runGpt(msg);
                } catch (ApiProxyException e) {
                  e.printStackTrace();
                }
                if (aiResponse != null) {
                  // Set the role of the AI response to the participantRole for correct display name
                  ChatMessage displayResponse =
                      new ChatMessage(participantRole, aiResponse.getContent());
                  javafx.application.Platform.runLater(
                      () -> {
                        appendChatMessage(displayResponse);
                        // Add AI response to this participant's history only
                        conversationHistories
                            .computeIfAbsent(participantRole, k -> new java.util.ArrayList<>())
                            .add(
                                getDisplayName(participantRole)
                                    + ": "
                                    + displayResponse.getContent());
                      });
                }
              }
            })
        .start();
  }

  @FXML
  private void onGoBack(ActionEvent event) {
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

  // === Private helper methods ===
  private String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    return PromptEngineering.getPrompt("chat.txt", map);
  }

  private void appendChatMessage(ChatMessage msg) {
    txtaChat.appendText(getDisplayName(msg.getRole()) + ": " + msg.getContent() + "\n\n");
  }

  String getDisplayName(final String role) {
    return "user".equals(role) ? "User" : DISPLAY_NAME_MAP.getOrDefault(role, "User");
  }

  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Prepend persona context to user message if needed
    String personaPrefix = "";
    if (msg.getRole().equals("user") && participantRole != null) {
      switch (participantRole) {
        case "aiDefendent":
          personaPrefix = "You are the AI defendant. Answer as MediSort-5: ";
          break;
        case "humanWitness":
          personaPrefix = "You are the human witness. Answer as Dr. Payne Gaun: ";
          break;
        case "aiWitness":
          personaPrefix = "You are the AI witness. Answer as PathoScan-7: ";
          break;
        default:
          break;
      }
      msg = new ChatMessage("user", personaPrefix + msg.getContent());
    }
    chatCompletionRequest.addMessage(msg);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      chatCompletionRequest.addMessage(result.getChatMessage());
      // Do not appendChatMessage here; let the background thread handle it
      return result.getChatMessage();
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    }
  }
}
