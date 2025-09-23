package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

/**
 * Controller for the AI Witness (PathoScan-7) chat interface.
 * Handles specific functionality for chatting with the AI witness.
 */
public class AiWitnessController extends ChatController {

  private static final String PARTICIPANT_ROLE = "aiWitness";

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
    return " You are the AI witness PathoScan-7, a Disease Spread AI. "
        + "You are an independent system that models contagion spread in the city. "
        + "You will testify that Patient A's illness had a very high transmission potential "
        + "in the care facility. You argue that MediSort-5's decision statistically protected more lives. "
        + "You speak in a precise, analytical manner with statistical data to support your testimony.";
  }
  
  @Override
  protected String getAdditionalContext() {
    return getStatisticalContext();
  }
  
  /**
   * Provides statistical analysis context specific to PathoScan-7's testimony.
   * This method can be used to generate context-specific responses.
   * 
   * @return statistical context for disease spread analysis
   */
  public String getStatisticalContext() {
    return "Current statistical analysis: Patient A's condition had a 73% probability of causing "
        + "a facility-wide outbreak affecting 15-25 individuals within 48 hours. "
        + "MediSort-5's prioritization algorithm prevented this scenario with 94.7% confidence.";
  }
}