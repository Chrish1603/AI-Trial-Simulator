package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.controllers.ChatController;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the Human Witness (Dr. Payne Gaun) chat interface.
 * Handles specific functionality for chatting with the human witness.
 */
public class HumanWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "humanWitness";

  @FXML private Slider agreementSlider;

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
    // Initialize any human witness specific UI components
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
  
  @FXML
  private void handleRectClick(MouseEvent event) throws IOException {
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    String participantId = clickedRectangle.getId();

    // Ensure a conversation history exists for this participant
    conversationHistories.computeIfAbsent(participantId, k -> new ArrayList<>());

    // Check if flashback should be shown first
    // if (!flashbackShown.contains(participantId)) {
    //   showFlashback(participantId, event);
    //   flashbackShown.add(participantId);
    //   return;
    // }

    // // Otherwise, go directly to chat
    // showChatInterface(participantId, event);
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

  /**
   * Gets the current agreement level from the slider.
   * This is specific to the human witness interface.
   * 
   * @return the agreement level as a percentage (0-100)
   */
  public double getAgreementLevel() {
    return agreementSlider != null ? agreementSlider.getValue() : 50.0;
  }
  
  /**
   * Sets the agreement level programmatically.
   * This can be used to update the slider based on conversation progress.
   * 
   * @param level the agreement level as a percentage (0-100)
   */
  public void setAgreementLevel(double level) {
    if (agreementSlider != null) {
      agreementSlider.setValue(Math.max(0, Math.min(100, level)));
    }
  }
}