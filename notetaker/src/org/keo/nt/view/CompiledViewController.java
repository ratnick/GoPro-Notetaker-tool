package org.keo.nt.view;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.keo.nt.MainApp;

import com.coremedia.iso.IsoFile;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class CompiledViewController {
		
	private MainApp mainApp;
	private Stage stage;
	
	private MediaPlayer mediaPlayer = null;
	private Duration duration = null;
	private Media media = null;
	private String initVideoFile;
	
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
	
	private EventHandler<?> eventHandler = new EventHandler<Event>() {
    	@Override
    	public void handle(Event event) {
    		System.out.println("STAGE EVENT: "+event.getEventType().getName());
    		if (event.getEventType() == WindowEvent.WINDOW_HIDING) {
    			mediaPlayer.dispose();
    		}
    	}
    };
    
    @FXML private void initialize() {
    }
	
    @FXML private void handleButtonStopRewind(ActionEvent e) {
		mediaPlayer.stop();
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
				buttonPlayPause.setText("Pause");
			}
			mediaPlayer.play();
		} else {
			mediaPlayer.pause();
			buttonPlayPause.setText("Play");
		}
	}

	public void handleTimeStamp(String fileName, long delayInSecs) {
		
		String filePath = makeFilePath(fileName);
		long duration = getDuration(filePath);
		long accumulatedDuration = duration;
		
		if (duration < delayInSecs) {
			// find file chapter and adjust delay
			int MAX_CHAPTERS = 10;
			int chapter = 1;
			while (chapter < MAX_CHAPTERS) {
				String nextChapter = String.format("GP%02d", chapter) + fileName.substring(4);
				System.out.println("CHAPTER: "+nextChapter);
				long chapterDuration = getDuration(makeFilePath(nextChapter));
				long chapterDelayInSecs = delayInSecs - accumulatedDuration;
				System.out.println("Duration: "+chapterDuration+" Delay: "+chapterDelayInSecs); 
				if (chapterDelayInSecs < chapterDuration) {
					// Found the chapter
					seekPending = true;
					seekDuration = new Duration(chapterDelayInSecs*1000);
					try {			
						initMediaPlayer(new File(makeFilePath(nextChapter)));
					} catch (Exception e) {
						mainApp.showError("Can't load video", "The default video file can't be found. Are you sure your note details are correct? Please check if it points to the correct folder and try again.");
					}
					break;
				}
				chapter++;
				accumulatedDuration += chapterDuration;
			}
	
		} else {
			seekPending = true;
			seekDuration = new Duration(delayInSecs*1000);
			try {			
				initMediaPlayer(new File(makeFilePath(fileName)));
			} catch (Exception e) {
				mainApp.showError("Can't load video", "The default video file can't be found. Are you sure your note details are correct? Please check if it points to the correct folder and try again.");
			}
		}		
	}

	protected void updateValues() {
	  if (labelTimeProgress != null && sliderTime != null) {
	     Platform.runLater(new Runnable() {
	        @SuppressWarnings("deprecation")
			public void run() {
	          Duration currentTime = mediaPlayer.getCurrentTime();
	          labelTimeProgress.setText(formatTime(currentTime, duration));
	          sliderTime.setDisable(duration.isUnknown());
	          if (!sliderTime.isDisabled() 
	            && duration.greaterThan(Duration.ZERO) 
	            && !sliderTime.isValueChanging()) {
	        	  sliderTime.setValue(currentTime.divide(duration).toMillis()
	                  * 100.0);
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

	private String makeFilePath(String file) {
    	return mainApp.getNote().getVideoLocation() + File.separator + file;
    }
    
	private Long getDuration(String file) {
	    IsoFile isoFile = null;
		try {
			isoFile = new IsoFile(file);
			long duration = isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
		    //Double durationInSecs = duration / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
		    System.out.println("DURATION in secs: "+duration);
		    isoFile.close();
		    return duration;
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	    return 0L;
	}
	
	public void initMediaPlayer(File file) throws Exception {
		
		try {
			
			if (media != null) {
				media = null;
			}
			
			if (!file.exists()) {
				Exception e = new Exception("Can't locate default file");
				throw e;
			} else {
				fileNameLabel.setText(file.getName());
				media = new Media(file.toURI().toURL().toExternalForm());
			}
			
			if (mediaPlayer != null) {
				mediaPlayer.dispose();
			}
			
			mediaPlayer = new MediaPlayer(media);
			mediaPlayer.setAutoPlay(true);						
			mediaView.setMediaPlayer(mediaPlayer );						
			buttonPlayPause.setText("Play");			
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
						buttonPlayPause.setText("Play");
					} else {
						buttonPlayPause.setText("Pause");
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
					buttonPlayPause.setText("Play");
				}
			});
			
			mediaPlayer.setOnStopped(new Runnable() {
				public void run() {					
					buttonPlayPause.setText("Play");
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
						buttonPlayPause.setText("Play");
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
			
		} catch (MalformedURLException e) {
			mainApp.showError("Couldn't compile the note", e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void configureView(MainApp mainApp, Stage stage) throws Exception{
		
		this.mainApp = mainApp;
		this.stage = stage;
		
		this.stage.setOnHiding((EventHandler<WindowEvent>) eventHandler);
				
		String timestamp = "";
		
		String doc = mainApp.getMainEditorViewController().getEditorText();
		int prevEndIndex = 0;
		int currentIndex = doc.indexOf("<<");
		Boolean EOF = false;
			
		while (!EOF) {
			
			timestamp = "";
			if (currentIndex == doc.length()) {
				EOF = true;
			}
			
			if (prevEndIndex < currentIndex) {
				Text text = new Text(doc.substring(prevEndIndex, currentIndex));
				double spw = scrollPane.getPrefWidth();
				System.out.println("Width: "+spw);
				text.wrappingWidthProperty().setValue(spw);
				vbox.getChildren().add(text);				
			} 
			
			int startIndex = currentIndex;
			int endIndex = doc.indexOf(">>", currentIndex);
			if (startIndex < endIndex) {
				endIndex = endIndex+2; 
				timestamp = doc.substring(startIndex, endIndex).trim();
				System.out.println(timestamp);
				
				if (timestamp != null && !timestamp.isEmpty()) {				
		        	String[] timestampComponents = timestamp.split("\\(");
		        	String hms = timestampComponents[0].substring(2);
		        	String videoName = timestampComponents[1].substring(0, timestampComponents[1].length()-3);
		        	if (initVideoFile == null || initVideoFile.isEmpty()){
		        		initVideoFile = videoName;
		        	}
		        	
		        	if (hms.matches("\\d{2}:\\d{2}:\\d{2}")) {
		        	
						Button button = new Button(timestamp);						
						button.setOnAction(new EventHandler<ActionEvent>() {
					        @Override
					        public void handle(ActionEvent event) {						        	
					        	
					    		String[] timeComponents = hms.split(":");
					    		long secs = Long.parseLong(timeComponents[0]) * 60 * 60; // add hours
					    		secs += Long.parseLong(timeComponents[1]) * 60; // add mins
					    		secs += Long.parseLong(timeComponents[2]); // add secs	  
					    		
					    		System.out.println("TIME: "+secs);
					        									            								
					            handleTimeStamp(videoName,secs);
					        }
					    });
						
						vbox.getChildren().add(button);
		        	}
				}
			}		
			prevEndIndex = currentIndex+timestamp.length();
			currentIndex = doc.indexOf("<<", prevEndIndex);
			if (currentIndex < 0) {
				currentIndex = doc.length();
			}
		} // end while
			
		vbox.setPadding(new Insets(5,5,5,5));
		scrollPane.setContent(vbox);
		
		String filePath = mainApp.getNote().getVideoLocation();		
		
		if (initVideoFile != null && filePath != null) {
			filePath = filePath + File.separator + initVideoFile;				
			fileNameLabel.setText(initVideoFile);
			
			System.out.println("Initialize media player: "+filePath);
			
			try {
				initMediaPlayer(new File(filePath));
			} catch (Exception e) {
				mainApp.showError("Can't load video", "The default video file can't be found. Are you sure your note details are correct? Please check if it points to the correct folder and try again.");
			}
		}
		
		vbox.setSpacing(10);		
    }

}
