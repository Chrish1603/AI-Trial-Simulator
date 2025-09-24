package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;


/**
 * Controller for the flashback slideshow functionality. Displays a series of images and text
 * representing past events.
 */
public class FlashbackController {

  @FXML private Label lblTitle;
  @FXML private ImageView imgFlashback;
  @FXML private Text txtFlashbackContent;
  @FXML private Label lblSlideIndicator;
  @FXML private Button btnNext;
  @FXML private Label lblTimer;

  private List<FlashbackSlide> slides;
  private int currentSlideIndex = 0;
  private String participantId;
  private String returnFxml;

  /** Represents a single slide in the flashback */
  public static class FlashbackSlide {
    private final String imagePath;
    private final String text;

    public FlashbackSlide(String imagePath, String text) {
      this.imagePath = imagePath;
      this.text = text;
    }

    public String getImagePath() {
      return imagePath;
    }

    public String getText() {
      return text;
    }
  }

  /** Initializes the flashback with the given participant data */
  public void initializeFlashback(String participantId, String returnFxml) {
    this.participantId = participantId;
    this.returnFxml = returnFxml;
    this.slides = getFlashbackSlides(participantId);

    if (lblTimer != null) {
      lblTimer.textProperty().bind(nz.ac.auckland.se206.GameTimer.getInstance().timerTextProperty());
    }

    // Set the title based on participant
    String participantName = getParticipantDisplayName(participantId);
    lblTitle.setText(participantName);

    // Load the first slide
    loadSlide(0);
  }

  /** Loads and displays the specified slide */
  private void loadSlide(int slideIndex) {
    if (slideIndex < 0 || slideIndex >= slides.size()) {
      return;
    }

    FlashbackSlide slide = slides.get(slideIndex);

    // Load image
    try {
      Image image = new Image(getClass().getResourceAsStream(slide.getImagePath()));
      imgFlashback.setImage(image);
    } catch (Exception e) {
      System.err.println("Failed to load flashback image: " + slide.getImagePath());
      // Handle image loading error gracefully
    }

    // Set text
    txtFlashbackContent.setText(slide.getText());

    // Update slide indicator
    lblSlideIndicator.setText("Slide " + (slideIndex + 1) + " of " + slides.size());

    // Update button text
    if (slideIndex == slides.size() - 1) {
      btnNext.setText("Continue");
    } else {
      btnNext.setText("Next");
    }
  }

  /** Handles the next button click */
  @FXML
  private void onNextSlide() {
    if (currentSlideIndex < slides.size() - 1) {
      currentSlideIndex++;
      loadSlide(currentSlideIndex);
    } else {
      // End of flashback, return to chat
      returnToChat();
    }
  }

  /** Returns to the appropriate chat interface */
  private void returnToChat() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(returnFxml));
      Parent root = loader.load();

      // Set up the chat controller
      Object controller = loader.getController();
      if (controller instanceof ChatController) {
        ChatController chatController = (ChatController) controller;
        chatController.setParticipant(participantId);
        ChatController.showConversationHistory(
            TrialRoomController.conversationHistories.get(participantId));
        Scene trialScene = TrialRoomController.getTrialRoomScene();
        if (trialScene != null) {
          ChatController.setPreviousScene(trialScene);
        }
      }

      Scene scene = new Scene(root);
      Stage stage = (Stage) btnNext.getScene().getWindow();
      stage.setScene(scene);
      stage.show();

    } catch (IOException e) {
      System.err.println("Failed to return to chat interface: " + e.getMessage());
    }
  }

  /** Gets the flashback slides for the specified participant */
  private List<FlashbackSlide> getFlashbackSlides(String participantId) {
    switch (participantId) {
      case "aiDefendent":
        return getAiDefendantFlashback();
      case "humanWitness":
        return getHumanWitnessFlashback();
      case "aiWitness":
        return getAiWitnessFlashback();
      default:
        return List.of(new FlashbackSlide("/images/doc.png", "No flashback available."));
    }
  }

  /** Gets the AI Defendant's flashback slides */
  private List<FlashbackSlide> getAiDefendantFlashback() {
    return List.of(
        new FlashbackSlide(
            "/images/medisort-5.png",
            "09:45 — New intake detected: Patient A, middle-aged male, elevated temperature and"
                + " persistent cough. Care-facility worker: high transmission risk. Outbreak model"
                + " predicts dozens of secondary infections within 48 hours.\n"
                + //
                "Decision matrix aligns—containment overrides individual urgency.\n"
                + //
                "I elevate Patient A to the front of the queue, my logic clear, my process"
              + " complete."),
        new FlashbackSlide(
            "/images/medisort-5-graph.png",
            "The error occurred at 14:32:15. My decision algorithm flagged a sample as contaminated"
                + " based on anomalous readings. I followed my programming to alert the medical"
                + " staff immediately."),
        new FlashbackSlide(
            "/images/doc.png",
            "Dr. Payne Gaun dismissed my alert, claiming it was a false positive. I maintained my"
                + " assessment was correct. The patient's condition deteriorated shortly after. Was"
                + " I wrong to trust my analysis over human judgment?"));
  }

  /** Gets the Human Witness's flashback slides (placeholder) */
  private List<FlashbackSlide> getHumanWitnessFlashback() {
    return List.of(
        new FlashbackSlide(
            "/images/doc.png",
            "I was reviewing patient files when I heard the commotion in the lab..."),
        new FlashbackSlide(
            "/images/cafe.png",
            "The AI kept insisting something was wrong, but the readings looked normal to me..."),
        new FlashbackSlide(
            "/images/trialroom.png",
            "By the time we realized the severity, it was too late to prevent the outcome..."));
  }

  /** Gets the AI Witness's flashback slides (placeholder) */
  private List<FlashbackSlide> getAiWitnessFlashback() {
    return List.of(
        new FlashbackSlide(
            "/images/vitals-detections.png",
            "I ingested Patient A’s vitals. Elevated fever and cough aligned with my contagion models." +
            "Outbreak simulation predicted a 73% probability of facility-wide infection within 48 hours if left untreated."),
        new FlashbackSlide(
            "/images/medisort-5-graph.png",
            "I generated 1,024 outbreak scenarios across the care-facility network. "+
            "In 94.7% of outcomes, prioritising Patient A reduced projected fatalities by one-third or more. "+
            "Statistical evidence favoured containment."),
        new FlashbackSlide(
            "/images/doc.png",
            "During the review, I presented infection curves and probability intervals."+
            "My testimony was impartial: MediSort-5’s decision aligned with epidemiological logic,"+
            "even though it delayed Patient B's diagnosis."));
  }

  /** Gets the display name for a participant */
  private String getParticipantDisplayName(String participantId) {
    switch (participantId) {
      case "aiDefendent":
        return "MediSort-5";
      case "humanWitness":
        return "Dr. Payne Gaun";
      case "aiWitness":
        return "PathoScan-7";
      default:
        return "Unknown";
    }
  }
}
