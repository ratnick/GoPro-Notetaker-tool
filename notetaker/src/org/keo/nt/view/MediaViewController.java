package org.keo.nt.view;

import java.io.File;

import org.keo.nt.MainApp;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class MediaViewController {
		
	private MainApp mainApp;
	private Stage stage;
	
	private MediaPlayer mediaPlayer = null;
	private Duration duration = null;
	private Media media = null;
	
	private Boolean atEndOfMedia = false;
	private Boolean stopRequested = false;
	private Boolean repeat = false;
	
	private Boolean seekPending = false;
	private Duration seekDuration;
	
	@FXML private MediaView mediaView;
	@FXML private Button buttonStopRewind;
	@FXML private Button buttonPlayPause;
	@FXML private Label labelTimeProgress;
	@FXML private Slider sliderTime;
	@FXML private VBox vbox;
	@FXML private Label fileNameLabel;
	@FXML private ScrollPane scrollPane;
	@FXML private AnchorPane anchorPane;
	@FXML private Slider sliderStereo;
	
//	private EventHandler<WindowEvent> eventHandler = new EventHandler<WindowEvent>() {
//    	@Override
//    	public void handle(WindowEvent event) {
//    		System.out.println("STAGE EVENT: "+event.getEventType().getName());
//    		if (event.getEventType() == WindowEvent.WINDOW_HIDING) {
//    			mediaPlayer.stop();
//    			mediaPlayer.dispose();
//    		}
//    	}
//    };
    
    private Object windowEventHandler(WindowEvent e) {
    	System.out.println("NEW STAGE EVENT: "+e.getEventType().getName());
		if (e.getEventType() == WindowEvent.WINDOW_HIDING) {
			mediaPlayer.stop();
			mediaPlayer.dispose();
			mainApp.setMediaViewController(null);
		}
		return null;
	}
    
    @FXML private void initialize() {
    }
    
    @FXML private void handleKeyPressed(KeyEvent e) {
    	if (e.isShiftDown() && e.getCode() == KeyCode.RIGHT) {
    		mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(10000)));
    	}
    	if (e.isShiftDown() && e.getCode() == KeyCode.LEFT) {
    		mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(new Duration(10000)));
    	}
    }
	
    @FXML private void handleButtonStopRewind(ActionEvent e) {
		mediaPlayer.stop();
	}
    
    @FXML private void handleRewind(ActionEvent e) {
    	mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(new Duration(10000)));
    }
    
    @FXML private void handleForward(ActionEvent e) {    	
    	mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(10000)));
    }

	@FXML private void handleButtonPlayPause(ActionEvent e) {
		
		Status status = mediaPlayer.getStatus();
		if (status == Status.UNKNOWN || status == Status.HALTED) {
			return;
		}
		if (status == Status.PAUSED || status == Status.READY || status == Status.STOPPED) {
			if (atEndOfMedia) {
				mediaPlayer.seek(mediaPlayer.getStartTime());
				atEndOfMedia = false;
				setPlayButton(false);
			}
			mediaPlayer.play();
		} else {
			mediaPlayer.pause();
			setPlayButton(true);
		}
	}

	protected void updateValues() {
	  if (labelTimeProgress != null && sliderTime != null) {
		  
	     Platform.runLater(new Runnable() {
	        //@SuppressWarnings("deprecation")
			public void run() {
	          Duration currentTime = mediaPlayer.getCurrentTime();
	          labelTimeProgress.setText(formatTime(currentTime, duration));
	          sliderTime.setDisable(duration.isUnknown());
	          if (!sliderTime.isDisabled() && duration.greaterThan(Duration.ZERO) && !sliderTime.isValueChanging()) {
	        	  //sliderTime.setValue(currentTime.divide(duration).toMillis() * 100.0);
	        	  sliderTime.setValue((currentTime.toSeconds()/duration.toSeconds())*100);	        	  	        	  
	          }
	          
	        }
	     });
	  }
	}

	private static String formatTime(Duration elapsed, Duration duration) {
	   int intElapsed = (int)Math.floor(elapsed.toSeconds());
	   int elapsedHours = intElapsed / (60 * 60);
	   if (elapsedHours > 0) {
	       intElapsed -= elapsedHours * 60 * 60;
	   }
	   int elapsedMinutes = intElapsed / 60;
	   int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 
	                           - elapsedMinutes * 60;
	
	   if (duration.greaterThan(Duration.ZERO)) {
	      int intDuration = (int)Math.floor(duration.toSeconds());
	      int durationHours = intDuration / (60 * 60);
	      if (durationHours > 0) {
	         intDuration -= durationHours * 60 * 60;
	      }
	      int durationMinutes = intDuration / 60;
	      int durationSeconds = intDuration - durationHours * 60 * 60 - 
	          durationMinutes * 60;
	      if (durationHours > 0) {
	         return String.format("%d:%02d:%02d/%d:%02d:%02d", 
	            elapsedHours, elapsedMinutes, elapsedSeconds,
	            durationHours, durationMinutes, durationSeconds);
	      } else {
	          return String.format("%02d:%02d/%02d:%02d",
	            elapsedMinutes, elapsedSeconds,durationMinutes, 
	                durationSeconds);
	      }
	      } else {
	          if (elapsedHours > 0) {
	             return String.format("%d:%02d:%02d", elapsedHours, 
	                    elapsedMinutes, elapsedSeconds);
	            } else {
	                return String.format("%02d:%02d",elapsedMinutes, 
	                    elapsedSeconds);
	            }
	        }
	    }
	
	private void setPlayButton(Boolean play) {
		String btnImage = play? "graphics/play.png" : "graphics/pause.png";
		buttonPlayPause.setStyle("-fx-background-image:url('" + btnImage + "');" +
									"-fx-background-repeat:no-repeat;" +
									"-fx-background-size:30,30;" +
									"-fx-background-color:transparent;");
	}

	private void initMediaPlayer(Duration delay, File videofile) throws Exception {
		
		//TODO test initMediaPlayer
		//String test = "http://10.5.5.9:8080/videos/DCIM/100GOPRO/GOPR0081.MP4";
		
		try {
			
			if (media != null) {				
				media = null;
			}
			
//			if (!videofile.exists()) {
//				Exception e = new Exception("Can't locate default file");
//				throw e;
//			} else {
//				fileNameLabel.setText(videofile.getName());
//				media = new Media(videofile.toURI().toURL().toExternalForm());
//			}
			fileNameLabel.setText(videofile.getName());
			if (videofile.getPath().substring(0, 4).equals("http")) {
				media = new Media(videofile.getPath());
			} else {
				media = new Media(videofile.toURI().toURL().toExternalForm());
			}
			
			
//			fileNameLabel.setText("Streaming");
//			media = new Media(test);
			
			
			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer.dispose();
			}
			
			seekPending = true;
			seekDuration = delay;
			
			mediaPlayer = new MediaPlayer(media);
			mediaPlayer.setAutoPlay(true);	
			
			mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Object>() {

				@Override
				public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
					updateValues();					
				}
			});
			
			mediaPlayer.setOnPlaying(new Runnable() {
				public void run() {
					if (stopRequested) {
						mediaPlayer.pause();
						stopRequested = false;
						setPlayButton(true);
					} else {
						setPlayButton(false);
						if (seekPending) {
							System.out.println("Status: "+mediaPlayer.statusProperty().get());
							mediaPlayer.seek(seekDuration);
							seekPending = false;
							seekDuration = null;
						}
					}
				}
			});
			
			mediaPlayer.setOnPaused(new Runnable() {
				public void run() {			
					setPlayButton(true);
				}
			});
			
			mediaPlayer.setOnStopped(new Runnable() {
				public void run() {
					setPlayButton(true);
				}
			});
			
			mediaPlayer.setOnReady(new Runnable() {
				public void run() {
					duration = mediaPlayer.getMedia().getDuration();
					updateValues();
				}
			});
			
			mediaPlayer.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
			
			mediaPlayer.setOnEndOfMedia(new Runnable() {
				public void run() {
					if (!repeat) {
						setPlayButton(true);
						stopRequested = true;
						atEndOfMedia = true;
						mediaPlayer.setStartTime(new Duration(0));
					}
				}
			});
			
			sliderTime.valueProperty().addListener(new ChangeListener<Number>() {
				public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
					if (sliderTime.isValueChanging()) {
					mediaPlayer.seek(duration.multiply(sliderTime.getValue()/100.0));
					updateValues();
					mediaPlayer.play();				
					}
				}
			});		
			
			sliderStereo.valueProperty().addListener(new ChangeListener<Number>() {
				public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
					if (sliderStereo.isValueChanging()) {						
						mediaPlayer.balanceProperty().setValue(new_val);
					}
				}
			});
			
			mediaView.setMediaPlayer(mediaPlayer );	
			setPlayButton(true);
			
		} finally {
			
		}
		//catch (MalformedURLException e) {
//			mainApp.showError("Couldn't compile the note", e.getMessage());
//		}
	}
	
	public void initMediaView(MainApp mainApp, Stage stage, Duration delay, File videofile) throws Exception{
		
		this.mainApp = mainApp;
		this.stage = stage;		
		//this.stage.setOnHiding((EventHandler<WindowEvent>) eventHandler);
		this.stage.setOnHiding(e->windowEventHandler(e));
			
		try {
			initMediaPlayer(delay, videofile);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public void updateMediaView(Duration delay, File videofile) {
		try {
			initMediaPlayer(delay, videofile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
