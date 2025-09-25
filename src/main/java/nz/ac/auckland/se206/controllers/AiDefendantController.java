package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the AI Defendant (MediSort-5) chat interface.
 * Handles specific functionality for chatting with the AI defendant.
 */
public class AiDefendantController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiDefendent";

  @FXML
  @Override
  public void initialize() throws ApiProxyException {
    super.initialize();
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
        + "Keep your responses concise and direct, limiting them to 3-4 sentences maximum.";
  }
}