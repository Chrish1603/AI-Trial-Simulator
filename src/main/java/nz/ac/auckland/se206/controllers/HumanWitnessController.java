package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.ImageView;
import nz.ac.auckland.se206.controllers.ChatController;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the Human Witness (Dr. Payne Gaun) chat interface.
 * Handles specific functionality for chatting with the human witness.
 */
public class HumanWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "humanWitness";
  private Boolean noteASeen = false;
  private Boolean noteBSeen = false;
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



  @Override
  protected String getParticipantRole() {
    return PARTICIPANT_ROLE;
  }

  @Override
  protected String getSystemPromptSuffix() {
    return " You are the human witness Dr. Payne Gaun, a Senior Clinic Physician. "
        + "You oversaw the post-incident review and believe AI over-relied on outbreak modelling "
        + "and undervalued individual acute cases. You argue that medical ethics require urgent "
        + "individual treatment when potential severe harm is present. You provide emotional "
        + "testimony on Patient B's deterioration and family distress.";
  }

}