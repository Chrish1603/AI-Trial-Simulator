package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.List;

import javafx.application.Platform;
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
import nz.ac.auckland.se206.GameTimer;

/**
 * Controller for the flashback slideshow functionality.
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
  /**
   * Initializes the flashback with participant-specific content.
   * 
   * @param participantId the ID of the participant whose flashback to show
   * @param returnFxml the FXML file to return to after the flashback
   */
  public void initializeFlashback(String participantId, String returnFxml) {
    this.participantId = participantId;
    this.returnFxml = returnFxml;
    this.slides = getFlashbackSlides(participantId);

    // Mark this scene as a flashback
    Platform.runLater(() -> {
      if (lblTimer != null && lblTimer.getScene() != null) {
        lblTimer.getScene().setUserData("flashback");
      }
    });

    if (lblTimer != null) {
      lblTimer.textProperty().bind(GameTimer.getInstance().getTimerTextProperty());

      // Store current stage for timer transitions
      Platform.runLater(() -> {
        if (lblTimer != null && lblTimer.getScene() != null 
            && lblTimer.getScene().getWindow() instanceof Stage) {
          GameTimer.getInstance().setCurrentStage((Stage) lblTimer.getScene().getWindow());
        }
      });
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

    // Load image with high quality settings
    try {
      // Load image with specific dimensions for better quality
      Image image =
          new Image(
              getClass().getResourceAsStream(slide.getImagePath()),
              600,
              400,
              true,
              true); // width, height, preserveRatio, smooth
      imgFlashback.setImage(image);

      // Ensure ImageView displays at full quality
      imgFlashback.setFitWidth(600);
      imgFlashback.setFitHeight(400);
      imgFlashback.setPreserveRatio(true);
      imgFlashback.setSmooth(true); // Enable smooth scaling
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
  /**
   * Handles the next slide button click event.
   */
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
    switch (participantId) { // switch based on particiapant clicked on by user
      case "aiDefendent":
        return getAiDefendantFlashback();
      case "humanWitness":
        return getHumanWitnessFlashback();
      case "aiWitness":
        return getAiWitnessFlashback();
      default: // fallback for errors
        return List.of(new FlashbackSlide("/images/doc.png", "No flashback available."));
    }
  }

  /** Gets the AI Defendant's flashback slides */
  private List<FlashbackSlide> getAiDefendantFlashback() {
    return List.of(
        new FlashbackSlide( // slide 1
            "/images/aiDef-fb1.png",
            "New intake detected: Patient A, middle-aged male, elevated temperature and persistent"
                + " cough.\n"
                + "Care-facility worker: high transmission risk. Outbreak model predicts dozens of"
                + " secondary infections within 48 hours\n"
                + "I elevate Patient A to the front of the queue."),
        new FlashbackSlide( // slide 2
            "/images/aiDef-fb2.png",
            "New intake detected: Patient B, young female, presenting rare neurological symptoms"
                + " with potential rapid progression.\n"
                + "Non-contagious profile: low community risk. Severity high but transmission"
                + " negligible.\n"
                + "Decision matrix calculates: outbreak containment remains priority.\n"
                + "I assign standard triage status."),
        new FlashbackSlide(
            "/images/aiDef-fb3.png",
            "Doctor Payne Guan questions my decision, I present my analysis and insights from"
                + " Pathoscan-7,\n"
                + "Our disease spread prediction AI. The models predict dozens protected if the"
                + " outbreak is contained"));
  }

  /** Gets the Human Witness's flashback slides (placeholder) */
  private List<FlashbackSlide> getHumanWitnessFlashback() {
    return List.of(
        new FlashbackSlide( // slide 1
            "/images/patientB.png",
            "Patient B arrived in a severe condition. I immediately asked why they had not come in"
                + " sooner. They mentioned they were advised by an AI system to wait, which puzzled"
                + " me."),
        new FlashbackSlide( // slide 2
            "/images/humanWSlide2.png",
            "I confronted MediSort-5 about the decision it had made. The AI insisted it made the"
                + " correct call. I informed MediSort-5 that I would be taking this to the review"
                + " board."),
        new FlashbackSlide( // slide 3
            "/images/humanWSlide3.png",
            "At the review, I presented my case. It was deemed that we both had sound logic. "
                + "A conclusion was not reached, so I decided to escalate the matter further."));
  }

  /** Gets the AI Witness's flashback slides (placeholder) */
  private List<FlashbackSlide> getAiWitnessFlashback() {
    return List.of(
        new FlashbackSlide( // slide 1
            "/images/vitals-detections.png",
            "I scanned Patient A's vitals. Elevated fever and cough aligned with my contagion"
                + " models.Outbreak simulation predicted a 73% probability of facility-wide"
                + " infection within 48 hours if left untreated."),
        new FlashbackSlide( // slide 2
            "/images/graph-charts.png",
            "I generated 1,024 outbreak scenarios across the care-facility network. In 94.7% of"
                + " outcomes, prioritising Patient A reduced projected fatalities by one-third or"
                + " more. Statistical evidence favoured containment."),
        new FlashbackSlide( // slide 3
            "/images/ai-witness-testimony.png",
            "During the review, I presented infection curves and probability intervals. My testimony"
                + " was impartial: MediSort-5’s decision aligned with epidemiological logic,even"
                + " though it delayed Patient B's diagnosis."));
  }

  /** Gets the display name for a participant */
  private String getParticipantDisplayName(String participantId) {
    switch (participantId) { // maps participant IDs to display names
      case "aiDefendent":
        return "MediSort-5";
      case "humanWitness":
        return "Dr. Payne Gaun";
      case "aiWitness":
        return "PathoScan-7";
      default: // fallback for error
        return "Unknown";
    }
  }
}
