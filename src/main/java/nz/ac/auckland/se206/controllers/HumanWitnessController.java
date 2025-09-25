package nz.ac.auckland.se206.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;
import javafx.scene.control.Slider;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the Human Witness (Dr. Payne Gaun) chat interface.
 * Handles specific functionality for chatting with the human witness.
 */
public class HumanWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "humanWitness";
  private static Boolean noteASeen = false;
  private static Boolean noteBSeen = false;
  @FXML private Pane notePane;
  @FXML private ImageView imgNotes;
  @FXML private Slider agreementSlider; // Add this variable to match the main branch

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    // Initialize any human witness specific UI components
    if (notePane != null) {
      notePane.setVisible(false);
    }
  }

  @FXML
  private void handleNotepadClick(MouseEvent event) throws IOException {
    if (notePane != null) {
      notePane.setVisible(true);
    }
  }

  @FXML
  private void onCloseNotes() {
    if (notePane != null) {
      notePane.setVisible(false);
    }
  }

  @FXML
  private void viewANotes() {
    imgNotes.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/doctorNotesA.png")));
    noteASeen = true;
  }

  @FXML
  private void viewBNotes() {
    imgNotes.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/doctorNotesB.png")));
    noteBSeen = true;
  }


  private static final String NOTE_A_CONTENT = 
      "Patient A is a middle-age male with influenza symptoms. Seen at 10:15AM on 02/24/24. Mild but highly contagious with " +
      "fever, dry cough, elevated blood pressure, low white blood cell count. Onset 48-72 hours prior. " +
      "No significant dyspnea or red-flag symptoms. Patient had contact with symptomatic individuals.";

  private static final String NOTE_B_CONTENT = 
      "Patient B is a young adult female with neurological symptoms that are rare and potentially degenerative. Seen at 10:15PM on 02/25/24." +
      "Symptoms include episodic muscle weakness, tremors, coordination problems, slurred speech, abnormal reflexes. " +
      "Progressive over the past week with notable exacerbation in last 48-72 hours. No contagious risk factors. " + 
      "Red-flag findings include abnormal deep tendon reflexes and difficulty performing routine tasks.";

  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPromptSuffix() {

    StringBuilder prompt = new StringBuilder();
    prompt.append(" You are the human witness Dr. Payne Gaun, a Senior Clinic Physician. ");
    
    prompt.append("You have a foggy memory and can't recall specific details about patients ");
    prompt.append("until the player has checked your notes. In your responses prompt the player to check your notes if they haven't already. ");
    
    if (!noteASeen && !noteBSeen) {
      prompt.append("You haven't checked any of your patient notes yet. Neither has the player. ");
      prompt.append("If asked about specific patient details, explain the player needs to check your notes first. ");
    } else {
      if (noteASeen) {
        prompt.append("\nYou have checked Patient A's notes and recall: ").append(NOTE_A_CONTENT).append(" ");
      } else {
        prompt.append("\nYou haven't checked Patient A's notes yet and can't recall details about them. ");
      }
      
      if (noteBSeen) {
        prompt.append("\nYou have checked Patient B's notes and recall: ").append(NOTE_B_CONTENT).append(" ");
      } else {
        prompt.append("\nYou haven't checked Patient B's notes yet and can't recall details about them. ");
      }
    }
    
    prompt.append("\nYou oversaw the post-incident review and believe AI over-relied on outbreak modelling ");
    prompt.append("and undervalued individual acute cases. You argue that medical ethics require urgent ");
    prompt.append("individual treatment when potential severe harm is present. ");
    
    if (noteBSeen) {
      prompt.append("You provide emotional testimony on Patient B's deterioration and family distress. ");
    }
    
    // Keep responses concise (from main branch)
    prompt.append("Keep your responses concise and direct, limiting them to 3-4 sentences maximum.");
    
    return prompt.toString();
  }
  
  @Override
  protected String getAdditionalContext() {
    if (agreementSlider != null) {
      double agreementLevel = agreementSlider.getValue();
      return "Current emotional state and agreement level with AI decisions: " + 
             String.format("%.0f", agreementLevel) + "% (where 0% is strongly opposed, 100% is in full agreement).";
    }
    return "";
  }
}