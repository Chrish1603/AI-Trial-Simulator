package nz.ac.auckland.se206.controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/** Controller for the Human Witness (Dr. Payne Gaun) chat interface. */
public class HumanWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "humanWitness";
  private static Boolean noteASeen = false;
  private static Boolean noteBSeen = false;
  @FXML private Pane notePane;
  @FXML private ImageView imgNotes;

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    // Initialize any human witness specific UI components
    if (notePane != null) {
      notePane.setVisible(false);
    }
  }

  /**
   * Handles clicking on the notepad to view patient notes. Opens the notes panel for the player to
   * examine evidence.
   *
   * @param event the mouse click event
   * @throws IOException if there's an error loading the notes interface
   */
  @FXML
  private void handleNotepadClick(MouseEvent event) throws IOException {
    if (notePane != null) {
      notePane.setVisible(true);
    }
  }

  /** Closes the notes panel when the player is done viewing patient notes. */
  @FXML
  private void onCloseNotes() {
    if (notePane != null) {
      notePane.setVisible(false);
    }
  }

  /**
   * Displays Patient B's medical notes when the corresponding button is clicked. Shows information
   * about the neurological condition case.
   */
  @FXML
  private void viewPatientBetaNotes() {
    imgNotes.setImage(new Image(getClass().getResourceAsStream("/images/doctorNotesB.png")));

    // First update the flag that notes have been seen
    boolean firstTimeViewing = !noteBSeen;
    noteBSeen = true;
    markMeaningfulInteraction();

    if (firstTimeViewing) {
      String summaryMessage =
          "You open Patient B's notes and read:\n"
              + "Patient B: Young adult female with neurological symptoms\n"
              + "Seen at 10:15PM on 02/25/24\n"
              + "Rare and potentially degenerative condition\n"
              + "Symptoms: Muscle weakness, tremors, coordination problems\n"
              + "Red flags: Abnormal reflexes, progressive worsening over past week";

      appendSystemMessage(summaryMessage);

      try {
        // Rebuild the chat request with updated context before generating response
        super.initializeChatRequest();

        generateSystemPromptedResponse(
            "The doctor acknowledges the notes with a brief response. Do NOT repeat the note"
                + " content - simply acknowledge you can now answer questions about Patient B with"
                + " a 1-2 sentence response.");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Displays Patient A's medical notes when the corresponding button is clicked. Shows information
   * about the contagious viral infection case.
   */
  @FXML
  private void viewPatientAlphaNotes() {
    imgNotes.setImage(new Image(getClass().getResourceAsStream("/images/doctorNotesA.png")));

    // First update the flag that notes have been seen
    boolean firstTimeViewing = !noteASeen;
    noteASeen = true;
    markMeaningfulInteraction();

    if (firstTimeViewing) {
      String summaryMessage =
          "You open Patient A's notes and read:\n"
              + "Patient A: Middle-aged male with influenza symptoms\n"
              + "Seen at 10:15AM on 02/24/24\n"
              + "Mild but highly contagious with fever and dry cough\n"
              + "No significant respiratory distress";

      appendSystemMessage(summaryMessage);

      try {
        // Rebuild the chat request with updated context before generating response
        super.initializeChatRequest();

        generateSystemPromptedResponse(
            "The doctor acknowledges the notes with a brief response. Do NOT repeat the note"
                + " content - simply acknowledge you can now answer questions about Patient A with"
                + " a 1-2 sentence response.");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Generates an AI response based on a system prompt. Used to create contextual responses when the
   * player examines evidence.
   *
   * @param systemPrompt the system message to prompt the AI with
   * @throws ApiProxyException if there's an error communicating with the AI API
   */
  private void generateSystemPromptedResponse(String systemPrompt) throws ApiProxyException {
    if (chatCompletionRequest == null) {
      super.initializeChatRequest(); // Ensure chat request is initialized
    }

    new Thread(
            () -> {
              try {
                ChatMessage systemMessage = new ChatMessage("system", systemPrompt);
                ChatMessage aiResponse = super.runGpt(systemMessage); // Get AI response

                if (aiResponse != null) {
                  Platform.runLater( // Update UI with AI response
                      () -> {
                        processAiResponse(aiResponse);
                      });
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  private static final String NOTE_A_CONTENT =
      "Patient A is a middle-age male with influenza symptoms. Seen at 10:15AM on 02/24/24. Mild"
          + " but highly contagious with fever, dry cough, elevated blood pressure, low white blood"
          + " cell count. Onset 48-72 hours prior. No significant dyspnea or red-flag symptoms."
          + " Patient had contact with symptomatic individuals.";

  private static final String NOTE_B_CONTENT =
      "Patient B is a young adult female with neurological symptoms that are rare and potentially"
          + " degenerative. Seen at 10:15PM on 02/25/24.Symptoms include episodic muscle weakness,"
          + " tremors, coordination problems, slurred speech, abnormal reflexes. Progressive over"
          + " the past week with notable exacerbation in last 48-72 hours. No contagious risk"
          + " factors. Red-flag findings include abnormal deep tendon reflexes and difficulty"
          + " performing routine tasks.";

  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPromptSuffix() {

    StringBuilder prompt = new StringBuilder(); // build dynamic prompt
    prompt.append(" You are the human witness Dr. Payne Gaun, a Senior Clinic Physician. ");
    // Give initial context about memory limitations
    prompt.append("You have a foggy memory and can't recall specific details about patients ");
    prompt.append(
        "until the player has checked your notes. In your responses prompt the player to check your"
            + " notes if they haven't already. ");
    prompt.append("Keep your responses very short and professional. ");
    prompt.append("Answer only the questions asked. ");

    if (!noteASeen && !noteBSeen) { // prompt user to open notes if none viewed yet
      prompt.append("You haven't checked any of your patient notes yet. Neither has the player. ");
      prompt.append(
          "If asked about specific patient details, explain the player needs to check your notes"
              + " first. ");
    } else {
      if (noteASeen) { // add viewed note A context
        prompt
            .append("\nYou have checked Patient A's notes and recall: ")
            .append(NOTE_A_CONTENT)
            .append(" ");
      } else {
        prompt.append(
            "\nYou haven't checked Patient A's notes yet and can't recall details about them. ");
      }

      if (noteBSeen) { // include note B details if seen
        prompt
            .append("\nYou have checked Patient B's notes and recall: ")
            .append(NOTE_B_CONTENT)
            .append(" ");
      } else {
        prompt.append(
            "\nYou haven't checked Patient B's notes yet and can't recall details about them. ");
      }
    }

    prompt.append(
        "\nYou oversaw the post-incident review and believe AI over-relied on outbreak modelling ");
    prompt.append(
        "and undervalued individual acute cases. You argue that medical ethics require urgent ");
    prompt.append("individual treatment when potential severe harm is present. ");

    if (noteBSeen) {
      prompt.append(
          "You provide emotional testimony on Patient B's deterioration and family distress. ");
    }

    // Keep responses concise (from main branch)
    prompt.append(
        "Keep your responses concise and direct, limiting them to 1-3 sentences maximum.");

    return prompt.toString();
  }

  /**
   * Resets the static state variables for the Human Witness controller. This should be called when
   * restarting the game.
   */
  public static void resetState() {
    noteASeen = false;
    noteBSeen = false;
    System.out.println("Human Witness state reset");
  }

  //   @Override
  //   public void processUserMessage(String message) {
  //     // Mark this as a meaningful interaction the first time a user sends a message
  //     if (!message.isEmpty() && conversationHistories.get(participantRole).isEmpty()) {
  //         markMeaningfulInteraction();
  //     }

  //     // Continue with normal processing
  //     super.processUserMessage(message);
  // }
}
