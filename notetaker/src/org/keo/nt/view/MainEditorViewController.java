package org.keo.nt.view;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.gopro.core.GoProHelper.GoProAPIversion;
import org.gopro.core.model.BacPacStatus;
import org.gopro.main.GoProApi;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyledTextArea;
import org.keo.nt.MainApp;
import org.keo.nt.TimeStamp;
import org.keo.nt.Utils;
import org.keo.nt.Session;
import org.reactfx.value.Val;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.Pos;

public class MainEditorViewController {

	private MainApp mainApp;
	private Boolean busy = false;
	private static Lock goproCmdMutex = new ReentrantLock(true);  // used when a sequence of commands is sent to gopro
	
	private final int MAX_BACKUPS = 10;
	
    ToggleGroup group = new ToggleGroup();
    ToggleButton btnCamera = new ToggleButton("");
    ToggleButton btnHarddisk = new ToggleButton("");
    TextField tfInput = new TextField();
    Button btnSelectDir = new Button("");	      
    
    String hdLocation = "";
    
	@FXML private ToggleGroup sourcegroup;
	
    @FXML private Button connectButton;
    @FXML private Circle connectIndicator;
    @FXML private Button startStopButton;
    @FXML private Label sessionTimeLabel;
        
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label progressStatusLabel;
    
	@FXML private VBox playBar;	    
	@FXML private BorderPane borderPane;
	
	@FXML private TextArea questions;
		
	@FXML private Button pullDownHandle;
	
	
    private Timeline sessionTimer;
    private Timeline autosaveTimer;
    
    private EventHandler<KeyEvent> keyEventHandler = new EventHandler<KeyEvent>() {
    	@Override
    	public void handle(KeyEvent event) {
    		if (event.isShiftDown()) {     			
				switch(event.getCode()) {
				case ENTER:
					
					insertTimeStamp();
					break;
					
				case A:
					
					System.out.println("LINE: "+getLineNumber(area.getCaretPosition()));
					
					
					break;
				default:
					break;
	    		}
    		} else if(event.isControlDown() && event.getCode() == KeyCode.F) {
    			mainApp.showAdvancedSearchView();
    		}
    	}
    };
    
    private void insertTimeStamp() {
    	if (busy) {
    		mainApp.updateStatus("Busy...can't insert timestamp now!");
    	} else {
    		busy = true;
    		
    		// Only proceed if a session is running		
    		if (mainApp != null && mainApp.getSession() != null) {

    			String videoName = mainApp.getSession().getVideoName();				    		
				if (videoName != null && !videoName.isEmpty()) {
							
					// CREATING TIMESTAMP
					// <TIMESTAMP timecode="00:12:45" filename="GOPR1234.MP4"/>
				    String timestamp = "\n<TIMESTAMP timecode=\""+mainApp.getSession().getSessionDurationHMS()+"\" filename=\""+videoName+"\"/> \n";
					
				    int caretPosition = area.getCaretPosition();					    		
					area.insertText(caretPosition, timestamp);
					TimeStamp ts = new TimeStamp(timestamp);
					ts.setStart(caretPosition);
					ts.setEnd(caretPosition+timestamp.length()-2);
					enableTimeStamp(ts);
							    	
				} else {		    			
					updateStatus("Can't find active recording, please (re)start session");
				}		
    		} else {
    			updateStatus("Can't find active recording, please (re)start session");
    		}
	    	busy = false;
    	}
    }
    
    @FXML private void handleInsertTimeStamp() {
    	insertTimeStamp();
    	area.requestFocus();
    }
    
    private long getLineNumber(int caretPosition) {    	
    	return countOccurences(area.getText().substring(0,caretPosition),'\n')+1;
    }
    
    private long countOccurences(String s, char c){
        return s.chars().filter(ch -> ch == c).count();
    }
    
    @FXML public void initialize() {
    	
    	autosaveTimer = new Timeline(new KeyFrame(Duration.millis(60000),ae -> autoSave()));
    	autosaveTimer.setCycleCount(Animation.INDEFINITE);
    	autosaveTimer.play();  
    	
    	setTimerLabel("REC");   
    	
    	questions.setWrapText(true);
    	    	
//    	harddiskButton.setUserData("harddisk");
//    	cameraButton.setUserData("camera");
//    	
//    	sourcegroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
//    	      public void changed(ObservableValue<? extends Toggle> ov, Toggle toggle,Toggle new_toggle) {
//    	    	  String selectedSource = new_toggle.getUserData().toString();
//    	          System.out.println(selectedSource);
//    	          if (selectedSource.equals("camera")) {
//    	        	  videoSourceTextField.setDisable(true);
//    	        	  videoSourceButton.setDisable(true);
//    	          } else {
//    	        	  videoSourceTextField.setDisable(false);
//    	        	  videoSourceButton.setDisable(false);
//    	          }
//    	      }
//    	    });
    	
    }
    
    class ArrowFactory implements IntFunction<Node> {
        private final ObservableValue<Integer> shownLine;

        ArrowFactory(ObservableValue<Integer> shownLine) {
            this.shownLine = shownLine;
        }

        @Override
        public Node apply(int lineNumber) {
            Polygon triangle = new Polygon(0.0, 0.0, 10.0, 5.0, 0.0, 10.0);
            triangle.setFill(Color.BLUE);

            ObservableValue<Boolean> visible = Val.map(
                    shownLine,
                    sl -> sl == lineNumber);

            triangle.visibleProperty().bind(
                    Val.flatMap(triangle.sceneProperty(), scene -> {
                        return scene != null ? visible : Val.constant(false);
                    }));

            return triangle;
        }
    }
    
    static class DefaultStyleDef {
        static final DefaultStyleDef NO_FORMATTING = new DefaultStyleDef();        

        private final TimeStamp timestamp;
        private final Utils.StyleType type;

        private DefaultStyleDef() {
            this(null, Utils.StyleType.TIMESTAMP);
        }

        DefaultStyleDef(TimeStamp timestamp, Utils.StyleType type) {
            this.timestamp = timestamp;
            this.type = type;
        }

        void applyToText(Text text, MainApp mainApp) {
        	switch(type) {
        	case DEFAULT:
        		text.setCursor(Cursor.DEFAULT);
        		text.setFill(Color.BLACK);
        		text.setUnderline(false);
        		text.setOnMouseClicked(null);
        		break;
        	case TIMESTAMP:
	            if(timestamp != null) {
	                text.setCursor(Cursor.HAND);
	                text.setFill(Color.BLUE);
	                text.setUnderline(true);
	                text.setOnMouseClicked(click -> {
	                	
	                	String folder = mainApp.gethdLocation(); //mainApp.getNote().getVideoLocation();
	                	
	                	//if (timestamp.isValid(folder)) {	                		
		                	Map<String,Object> adjustedTimestamp = Utils.handleChapters(folder, timestamp.getFilename(), Utils.convertHMStoSec(timestamp.getTimecode()));	
		                	if (adjustedTimestamp.containsKey("chapter") && adjustedTimestamp.containsKey("delay")) {
			                	//String videofilePath = Utils.makeVideoPath(mainApp.getNote().getVideoLocation(), adjustedTimestamp.get("chapter").toString());
		                		String videofilePath = Utils.makeVideoPath(mainApp.gethdLocation(), adjustedTimestamp.get("chapter").toString());
			                	Duration delay = (Duration)adjustedTimestamp.get("delay");
			                	if (videofilePath.substring(0, 4).equals("http")) {
			                		showMediaView(mainApp, delay, new File(videofilePath));
			                	} else {
			                		// Check if local path exists
			                		if (new File(videofilePath).exists()) {
			                			showMediaView(mainApp, delay, new File(videofilePath));
			                		} else {
			                			mainApp.showError("Invalid local path", "The local path specified is NOT valid, please correct it and try playing the video again.");
			                		}
			                	}			                    
		                	} else {
		                		mainApp.showError("Invalid timestamp", "Something is wrong with the time stamp, maybe the time code exceeds the length of the referred video?.");
		                	}
	                	//} else {
	                	//	mainApp.showError("Invalid timestamp", "Something is wrong with the time stamp, perhaps the filepath is incorrect?.");
	                	//}
	                });
	            }
	            break;
        	case FONTCOLOR:
        		text.setFill(Color.YELLOW);
        		break;        	
        	}
        }
        
    }

    StyledTextArea<DefaultStyleDef> area = new StyledTextArea<DefaultStyleDef> ( DefaultStyleDef.NO_FORMATTING, (text, style) -> style.applyToText(text, mainApp));
          
    {
    	area.setWrapText(true);    	
    }    
    
    public static void showMediaView(MainApp mainApp, Duration delay, File videofile) {
		
        try {        	
        	if (mainApp.getMediaViewController() == null) {
	        	FXMLLoader loader = new FXMLLoader();
		        loader.setLocation(MainApp.class.getResource("view/MediaView.fxml"));
		        AnchorPane page = (AnchorPane) loader.load();
	
		        Stage mediaViewerStage = new Stage();
		        mediaViewerStage.setTitle("Media Viewer");
		        mediaViewerStage.initModality(Modality.WINDOW_MODAL);
		        mediaViewerStage.setResizable(false);
		        Scene scene = new Scene(page);
		        mediaViewerStage.setScene(scene);
	
		        MediaViewController controller = loader.getController();
		        controller.initMediaView(mainApp, mediaViewerStage, delay, videofile);
		        mainApp.setMediaViewController(controller);
		        
				mediaViewerStage.show();
				
        	} else {
        		mainApp.getMediaViewController().updateMediaView(delay, videofile);
        	}
		} catch (Exception e) {	    				
			mainApp.showError("Couldn't open video", "Please check the note details and try again. Usually this problem is caused by an incorrect path to the video folder.");
		}
	}
    
    private void initLayout() {
    	
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        area.addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);        

        borderPane.setCenter(area);
        
        IntFunction<Node> numberFactory = LineNumberFactory.get(area);
        IntFunction<Node> arrowFactory = new ArrowFactory(area.currentParagraphProperty());
        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                numberFactory.apply(line),
                arrowFactory.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);
            return hbox;
        };
        area.setParagraphGraphicFactory(graphicFactory);        
        area.requestFocus();   
        
        pullDownHandle.setStyle("-fx-background-image:url('/graphics/pulldown_handle.png');"
        		+ "-fx-background-color: transparent;"
        		+ "-fx-background-repeat: no-repeat;");
        
        Label lblHeader = new Label("Choose where to play video from...");
        lblHeader.setStyle("-fx-font: 18px 'Arial'; -fx-text-fill:white;");        
        
        AnchorPane apPullDownArea = new AnchorPane();
        {
	        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
				
	        	public void changed(ObservableValue<? extends Toggle> observable, Toggle oldToggle, Toggle newToggle){
	        		
	        		if (newToggle != null && newToggle.getUserData() != null) {
	        			String toggle = newToggle.getUserData().toString();
	        			if (toggle.equals("harddisk")) {
	        				//tfInput.setDisable(false);	        				
        					tfInput.setText(mainApp.getNote().getVideoLocation());
        					mainApp.sethdLocation(mainApp.getNote().getVideoLocation());
        					btnSelectDir.setDisable(false);
        					
	        			} else if (toggle.equals("camera")) {	
	        				mainApp.sethdLocation("http://10.5.5.9:8080/videos/DCIM/100GOPRO/");
	        				tfInput.setText("http://10.5.5.9:8080/videos/DCIM/100GOPRO/");
	        				//tfInput.setDisable(true);
	        				btnSelectDir.setDisable(true);
	        			}	        			
	        		}
				}
			});
	        
	        btnCamera.setToggleGroup(group);
	        btnCamera.setLayoutX(0);
	        btnCamera.setPrefWidth(100.0);
	        btnCamera.setPrefHeight(30.0);
	        //btnCamera.getStyleClass().add("buttoncam");
	        btnCamera.setId("buttoncam");
	        btnCamera.setUserData("camera");
	        
	        btnHarddisk.setToggleGroup(group);
	        btnHarddisk.setSelected(true);
	        btnHarddisk.setLayoutX(100);
	        btnHarddisk.setPrefWidth(100.0);
	        btnHarddisk.setPrefHeight(30.0);
	        //btnHarddisk.getStyleClass().add("button10030");
	        btnHarddisk.setId("buttonhd");
	        btnHarddisk.setUserData("harddisk");
	        
	        tfInput.setPromptText("/Location/videos/on/harddisk");	        	        
	        //tfInput.setPrefWidth(500);
	        tfInput.setPrefHeight(30);
	        tfInput.setStyle("-fx-background-color:#ffffff,#ffffff;");	  
	        tfInput.textProperty().addListener(new ChangeListener<String>() {

				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					mainApp.sethdLocation(newValue);
					System.out.println(group.getSelectedToggle().getUserData().toString());
					if (group.getSelectedToggle().getUserData().toString().equals("harddisk")) {						
						if (mainApp != null && mainApp.getNote() != null) {
							mainApp.getNote().setVideoLocation(newValue);
						}
					}					
				}
	        	
	        });
	        
	        btnSelectDir.setPrefWidth(60);
	        btnSelectDir.setPrefHeight(30.0);
	        btnSelectDir.setId("open");
	        btnSelectDir.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					DirectoryChooser chooser = new DirectoryChooser();        		
			    	chooser.setTitle("Source directory");		
					File selectedDirectory = chooser.showDialog(mainApp.getPrimaryStage());
					if (selectedDirectory != null) {
						tfInput.setText(selectedDirectory.getPath().toString());						
					}					
				}
	        	
	        });
	        //btnSelectDir.getStyleClass().add("button6030");
	        
	        AnchorPane.setRightAnchor(btnSelectDir, 10.0);
	        AnchorPane.setRightAnchor(tfInput, 70.0);
	        AnchorPane.setLeftAnchor(tfInput, 215.0);
	        
	        apPullDownArea.getChildren().addAll(btnCamera,btnHarddisk,tfInput,btnSelectDir);
        }        
        
        BorderSlideBar topFlapBar = new BorderSlideBar(75, pullDownHandle, Pos.TOP_LEFT, lblHeader,apPullDownArea);
        borderPane.setTop(topFlapBar);

        connectToGP();
        monitorCamRecordingStatus();
    }    

    public void deleteQuestionsText() {
    	questions.deleteText(0, questions.getLength());    	
    }
    
    public void deleteEditorText() {
    	area.deleteText(0, area.getLength());
    }
    
    public void insertEditorText(String text) {
    	area.deleteText(0, area.getLength());
    	area.replaceText(text);
    	
    	if (mainApp != null && mainApp.getNote() != null) {
    		tfInput.setText(mainApp.getNote().getVideoLocation());
    		mainApp.sethdLocation(mainApp.getNote().getVideoLocation());
    	}
    }
    
    public void markTimestamps(String text) {
    	
    	List<TimeStamp> timestamps = Utils.getTimeStamps(text);
    	timestamps.forEach(obj-> enableTimeStamp(obj));
    	
	}
    
    private void enableTimeStamp(TimeStamp timestamp) {    	
        if(timestamp.getStart() < timestamp.getEnd()) {			         
        	area.setStyle(timestamp.getStart(), timestamp.getEnd(), new DefaultStyleDef(timestamp, Utils.StyleType.TIMESTAMP));
        }
    }
    
    public String getEditorText() {
    	return area.getText();
    }
    
    public void insertQuestions(String text) {
    	questions.setText(text);
    }
    
    public String getQuestions() {
    	return questions.getText();
    }
        
    public MainEditorViewController() {    	
    }
    
    public void setProgressValue(float value) {
    	this.progressIndicator.setProgress(value);
    }
    
    public ProgressIndicator getProgressIndicator() {
    	return this.progressIndicator;
    }
    
    public Label getProgressStatusLabel() {
    	return this.progressStatusLabel;
    }
    
    public Boolean updateStatus(String txt) {
    	try {
    		if (!this.progressStatusLabel.textProperty().isBound()) {
    			this.progressStatusLabel.setText(txt);
    		} else {
    			return false;
    		}
    	} catch(RuntimeException e) {
    		return false;
    	}
    	return true;
    }
    
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;        
       
        initLayout();
        
        mainApp.addIsConnectedListener(new ChangeListener<Object>() {
			
        	public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue){
				Boolean isConnected = (Boolean)newValue;
				if (isConnected) {
					setConnectedStatus(true);
				} else {
					setConnectedStatus(false);
				}
			}
		});
    }
    
    public void setConnectedStatus(Boolean connected) {
    	
    	if (connected) {
    		//connectButton.setText("Disconnect");
    		//connectIndicator.setFill(Color.GREEN);
    		System.out.println("Connected");
    		mainApp.setIsConnected(true);
    		startStopButton.setDisable(false);
    		connectButton.setStyle("-fx-background-image:url('graphics/wifi_connected.png');"
    				+ "-fx-background-repeat:no-repeat;"
    				+ "-fx-background-size:55,55;"
    				+ "-fx-background-color:transparent;");
    	} else {
    		//connectButton.setText("Connect");
    		//connectIndicator.setFill(Color.RED);
    		System.out.println("Disconnected");
    		mainApp.setIsConnected(false);
    		startStopButton.setDisable(true);
    		connectButton.setStyle("-fx-background-image:url('graphics/wifi_disconnected.png');"
    				+ "-fx-background-repeat:no-repeat;"
    				+ "-fx-background-size:55,55;"
    				+ "-fx-background-color:transparent;");
    	}    	
    }
    
    public void disableConnectButton(Boolean disable) {
    	if (disable) {
    		System.out.println("DISABLE");
    		connectButton.setDisable(true);
//    		connectButton.setStyle("-fx-background-image:url('graphics/wifi_disabled.png');"
//    				+ "-fx-background-repeat:no-repeat;"
//    				+ "-fx-background-size:55,55;"
//    				+ "-fx-background-color:transparent;");
    	} else {
    		System.out.println("ENABLE");
    		connectButton.setDisable(false);
//    		connectButton.setStyle("-fx-background-image:url('graphics/wifi_connect.png');"
//    				+ "-fx-background-repeat:no-repeat;"
//    				+ "-fx-background-size:55,55;"
//    				+ "-fx-background-color:transparent;");
    	}
    }
    
    public void disableStartStopButton(Boolean disable) {
    	startStopButton.setDisable(disable);
    }
    
    public void setTimerLabel(String text) {

		Platform.runLater(new Runnable() {
               @Override public void run() {
            	  sessionTimeLabel.setText(text);
               }
           }); 	
    }

    private void displayMessageBox(String title, String s, Boolean waitForReply) {
    	if (waitForReply) {
	    	Platform.runLater(new Runnable() {
				@Override public void run() {
		    		mainApp.showError(title, s);
		        }
		    });
    	} else {
	    	Platform.runLater(new Runnable() {
				@Override public void run() {
//		    		updateStatus("Wrong Wifi connected.");
		    		mainApp.showError(title, s);
		        }
		    });
    	}
    }

    class MissingTextPrompt implements Callable<String> {
    	  private TextField textField;

    	  @Override public String call() throws Exception {
    	    final Stage dialog = new Stage();
    	    //dialog.setScene(createDialogScene());
    	    dialog.showAndWait();
    	    return textField.getText();
    	  }
    	 
    	}
    
    public MainApp getMainApp() {
    	return this.mainApp;
    }
    
    @FXML private void handleConnectButton() {
    	connectToGP();
    }
    	
    private boolean reconnectToWifi()  {

    	String wifiResult = "";
    	
		// if we already have a valid ssid (=camName), we should use that and try to reconnect. If not, we should start from scratch. 
		String camName = mainApp.getGoPro().getHelper().getSSID();
		if (!mainApp.getGoPro().checkAndSetSSID(camName)) {
			displayMessageBox("Connect to Wifi", "Connect to gopro wifi before continuing", true);
    		camName = org.keo.nt.utils.WifiOperations.GetWifiInfo();  //fetch gopro name from SSID
		}
   		if ( mainApp.getGoPro().checkAndSetSSID(camName) ) {
   			wifiResult = org.keo.nt.utils.WifiOperations.ConnectToGoProWifi(camName); 
   		} else {
    		displayMessageBox("Connect to Wifi", "Connect to GoPro's hotspot and click the connect button again", true);
   		}
   		if (wifiResult.equals("")) {
   			return true;
   		} else {
   			displayMessageBox("Could not connect to Wifi", wifiResult, true);
   			return false;
   		}
    }

  
    private void connectToGP() {
    	System.out.println("CONNECT TO GOPRO");
    	
    	if (!mainApp.getIsConnected()) {
    		disableConnectButton(true);
    	}
    	
    	final Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() throws InterruptedException {
            	//boolean result = false;

            	goproCmdMutex.lock();
            	try {			            			                		
	            	if (mainApp.getIsConnected()) {
	            		updateProgress(0,2);
	            		if (mainApp.getSession() != null && mainApp.getSession().isRunningProperty().getValue()) {
	            			// is running, initiate STOP
	            			updateMessage("Stopping...");
	            			try {
								mainApp.getGoPro().stopRecord();
							} catch (Exception e) {
								updateMessage("Could NOT stop recording!");
								Thread.sleep(2000);
							}
	                		mainApp.getSession().setIsRunning(false);                		
	                		sessionTimer.stop();                		
	            		}
	        			updateProgress(1,2);
	        			updateMessage("Disconnecting...");
	            		Platform.runLater(new Runnable() {
	     	               @Override public void run() {
	     	            	  //System.out.println("Run Later");
	     	            	  //startStopButton.setText("Start session");
	     	            	  setConnectedStatus(false);
	     	               }
	     	           });
	            		updateProgress(2,2);
	        			updateMessage("Disconnected!");
	            	} else {
	            		//try to connect
	            		try {	
	            			updateProgress(0,4);
	            			updateMessage("Connecting to GoPro hotspot...(takes ~20 seconds)");
		            		if ( reconnectToWifi() ) {
		            			updateMessage("Powering...");
		            			if (mainApp.getGoPro().powerAndWaitUntilIsReady()) {
		            				updateProgress(1,4);
		                        	updateMessage("Checking status...");
		                        	if (mainApp.getGoPro().isGoProRecording()) {   
		                        		mainApp.getGoPro().stopRecord();
		                        	}
		            				updateProgress(2,4);
		                        	updateMessage("Preparing configuration...");
		            				Thread.sleep(2000);
		            				updateProgress(3,4);
		                        	updateMessage("Sending configuration...");
		            				mainApp.getGoPro().sendConfiguration();   
		            				updateProgress(4,4);
		            				String gpName = mainApp.getGoPro().getAPIversion().toString();
		                        	updateMessage("Connected to " + gpName);
		            			}
		            			Platform.runLater(new Runnable() {
		            				@Override public void run() {
									   //System.out.println("Run Later");
									   setConnectedStatus(true);  
		            				}
		            	        });
	            	    	} else {
	            	    		displayMessageBox("Wifi problems.", "wifiRES", true);
	            	    	}
	            		} catch (Exception e1) {				
	         				updateMessage("Error connecting! Enable Wifi and click wifi icon.");
	        				updateProgress(0,0);
	        				Platform.runLater(new Runnable() {
	          	               @Override public void run() {
	          	            	  disableConnectButton(false);
	          	               }
	          	           }); 
	        			}    		
	            	}            	         
            	} catch(Exception e) {
            		;
            	} finally {
            		goproCmdMutex.unlock();
            	}
              return true; // always
            }
          };
          
          mainApp.getProgressStatusLabel().textProperty().bind(task.messageProperty());
          mainApp.getProgressIndicator().progressProperty().bind(task.progressProperty());
          
          task.stateProperty().addListener(new ChangeListener<Worker.State>() {
              @Override public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                	Platform.runLater(new Runnable() {
     	               @Override public void run() {
     	            	  disableConnectButton(false);
     	            	  mainApp.getProgressStatusLabel().textProperty().unbind();
     	            	  mainApp.getProgressIndicator().progressProperty().unbind();
     	               }
     	           });  
                }
              }
            });
          
          new Thread(task).start();
    }
    
    private String startRecording() {  // goproCmdMutex protected

    	String videoName = "";
    	goproCmdMutex.lock();
    	try {
	    	if (  mainApp.getSession() == null || 
	    		 (mainApp.getSession() != null 	&& !mainApp.getSession().isRunningProperty().getValue())  ) {    // check that we are not already recording (logically)...
	    		// For HERO5: The camera cannot reply to media list requests during recording, so we have to get the file name in advance
	    		if (mainApp.getGoPro().getAPIversion() != GoProAPIversion.HERO3) {
					videoName = mainApp.getGoPro().getActiveRecordingFile();
					if (videoName.equals("GOPR0001.MP4")) {
			    		System.out.println("Can't use an empty SD card. Re-start recording.");
				    	mainApp.getGoPro().startRecord();	 
				    	mainApp.getGoPro().stopRecord();	 
						mainApp.getSession().setIsRunning(false);
						Thread.sleep(1000);
						videoName = "";
					}
	    		}
		    	mainApp.getGoPro().startRecord();			  // start the recording 
				mainApp.getSession().setIsRunning(true);
		    	if (mainApp.getGoPro().isGoProRecording()) {  // and check physically if we succeeded
		    		if (mainApp.getSession() != null) {
						mainApp.getSession().setStartInstant(Instant.now());                		
						connectIndicator.setFill(Color.RED);
						sessionTimer = new Timeline(new KeyFrame(Duration.millis(1000),ae -> updateLabel()));
						sessionTimer.setCycleCount(Animation.INDEFINITE);
						sessionTimer.statusProperty().addListener(new ChangeListener<Status>() {
				
							@Override
							public void changed(
									ObservableValue<? extends Status> observable,
									Status oldValue, Status newValue) {
								if (newValue == Status.STOPPED) {
									connectIndicator.setVisible(true);
								}
							}
							
						});
						sessionTimer.play();
						
			    		if (mainApp.getGoPro().getAPIversion() == GoProAPIversion.HERO3) {
				    		// For HERO3: The camera CAN reply to media list requests during recording
							Thread.sleep(1000);
							videoName = mainApp.getGoPro().getActiveRecordingFile();
			    		}
						mainApp.getSession().setVideoName(videoName);
						return videoName;
			    	}
		    	} else {
		    		System.out.println("StartRecording: setIsRunning(false). Start cmd to gopro did not succeed.");
					mainApp.getSession().setIsRunning(false);
					Thread.sleep(1000);
		    	}
	    	} else {
	    		System.out.println("StartRecording: already recording.");
	    	}

    	} catch(Exception e) {
    		
    		Platform.runLater(new Runnable() {
	               @Override public void run() {
	            	   
	            	   setConnectedStatus(false);
	            	   disableStartStopButton(false);
	            	   
	            	   if (mainApp.getSession() != null){
	            		   mainApp.getSession().setIsRunning(false);
	            	   }
	            	   if (sessionTimer != null) {
	            		   sessionTimer.stop();
	            	   }
               			            	   
	            	   if (!mainApp.updateStatus("Error! Can't reach camera.")) {
	            		   mainApp.showError("Connection error", "A connection error occurred, please manually check if the session was started or stopped correctly. (4)");
	            	   }
	            	   
	               }
 			}); 
    	} finally {
    		goproCmdMutex.unlock();
    	}
    	return videoName;
    }
    
    public Boolean stopRecording() {  // goproCmdMutex protected    	

    	Boolean result = false;

    	goproCmdMutex.lock();
    	try {			            			                		
    		mainApp.getSession().setIsRunning(false);
    		connectIndicator.setFill(Color.GRAY);
    		sessionTimer.stop();
    		setTimerLabel("REC");
    		saveBackupNote();
    		mainApp.getGoPro().stopRecord();
    		result = true;
			
    	} catch(Exception e) {
  		
    		Platform.runLater(new Runnable() {
	               @Override public void run() {
	            	   
	            	   setConnectedStatus(false);
	            	   disableStartStopButton(false);
	            	   
	            	   if (mainApp.getSession() != null){
	            		   mainApp.getSession().setIsRunning(false);
	            	   }
	            	   if (sessionTimer != null) {
	            		   sessionTimer.stop();
	            	   }
               			            	   
//	            	   if (!mainApp.updateStatus("Error! Can't reach camera.")) {
//	            		   mainApp.showError("Connection error.", "A connection error occurred, please manually check if the session was started or stopped correctly. (1)");
//	            	   }
	            	   
	               }
 			}); 
    	} finally {
    		goproCmdMutex.unlock();
    	}
    	return result;
    }
    
    @FXML private void handleStartStopSession() {
    	doStartStopCam();
    }
    
    public void doStartStopCam() {
    	
    	disableStartStopButton(true);
    	
    	final Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() throws InterruptedException {
            	
            	updateProgress(-1,1);            	
            	
            	try {
            		
            		if (mainApp.getSession() == null) {
            			mainApp.setSession(new Session());   
            		}
            		
            		if (mainApp.getSession().isRunningProperty().getValue()) {
            			// is running, initiate STOP
            			updateMessage("Stopping...");
            			Boolean stat = stopRecording();
            			updateProgress(1,1);
            			
            			if (stat) {            				
                    		updateMessage("Stopped.");
            			} else {            				
                    		updateMessage("Couldn't reach the camera.");
            			}

                		saveBackupNote();
                		
            		} else {
            			updateMessage("Starting...");
            			
            			String videoName = startRecording();

                		updateProgress(1,1);
                		
                		if (videoName != "") {
            				updateMessage("Started session: "+videoName);
                		} else {
                			updateMessage("");
                		}
            		}
            		
        		} catch (Exception e) {
        			Platform.runLater(new Runnable() {
     	               @Override public void run() {
     	            	   
     	            	   setConnectedStatus(false);
     	            	   disableStartStopButton(false);
     	            	   
     	            	   if (mainApp.getSession() != null){
     	            		   mainApp.getSession().setIsRunning(false);
     	            	   }
     	            	   if (sessionTimer != null) {
     	            		   sessionTimer.stop();
     	            	   }
	                  		
     	            	   updateMessage("");
     	            	   if (!mainApp.updateStatus("Error! Can't reach camera.")) {
     	            		   mainApp.showError("Connection error", "A connection error occurred, please manually check if the session was started or stopped correctly. (2)");
     	            	   }
     	            	   
     	               }
        			});        	    		 		        	
        		}
            	
              return true;
            }
          };
          
          progressStatusLabel.textProperty().bind(task.messageProperty());
          progressIndicator.progressProperty().bind(task.progressProperty());
          
          task.stateProperty().addListener(new ChangeListener<Worker.State>() {
              @Override public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                	Platform.runLater(new Runnable() {
      	               @Override public void run() {
      	            	 if (mainApp.getSession().isRunningProperty().getValue()) {
                     		//startStopButton.setText("Stop session");
                     	} else {
                     		//startStopButton.setText("Start session");
                     	}
      	            	disableStartStopButton(false);
      	            	progressStatusLabel.textProperty().unbind();
      	            	progressIndicator.progressProperty().unbind();
      	               }
         			});                                
                } else {
                	//System.out.println("THREAD "+newState.name());
                }
              }
            });
          
          new Thread(task).start();
    	
    }
    
    private void updateLabel() {
    	this.sessionTimeLabel.setText(mainApp.getSession().getSessionDurationHMS());
    	this.connectIndicator.setVisible(!connectIndicator.isVisible());
    }
    
    private void autoSave() {
    	File noteFile = this.mainApp.getNoteFileFromPrefs();
        if (noteFile != null/* && mainApp.getNote().isReadyToSave()*/) { 
        	mainApp.saveNoteDataToFile(noteFile);
        	if (!this.progressStatusLabel.textProperty().isBound()) {
        		this.progressStatusLabel.setText("Autosaved!");
        	}
        	saveBackupNote();
        }
    }
    
    private void saveBackupNote() {
    	
		File noteFile = this.mainApp.getNoteFileFromPrefs();
		File backupFolder = null;
		String backupFilePath = null, backupFileName = null;
		
        if (noteFile != null) {
    		String noteFilePath = noteFile.getAbsolutePath();
    		
    		int fileIndex = noteFilePath.contains(File.separator) ? noteFilePath.lastIndexOf(File.separator) : 0;
    		if (fileIndex > 0) {
    			
    			DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
    			Date date = new Date();        			
    			
    			backupFileName = noteFilePath.substring(fileIndex+1);
    			
    			int extIndex = backupFileName.contains(".") ? backupFileName.lastIndexOf('.') : 0;
    			if (extIndex > 0) {
    				backupFilePath = noteFilePath.substring(0, fileIndex) + File.separator + "BACKUP" + File.separator;
    				backupFolder = new File(backupFilePath);
    				if (!new File(backupFilePath).exists()) {
    					new File(backupFilePath).mkdirs();
    				}
    				backupFilePath = backupFilePath + backupFileName.substring(0, extIndex) + "_BACKUP_" + dateFormat.format(date) + backupFileName.substring(extIndex);
    				//System.out.println("BACKUP: "+backupFilePath);
    			}
    			
        		File backupFile = new File(backupFilePath);
        		
        		try {            			
					Files.copy(noteFile.toPath(), backupFile.toPath(), REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
        		            		
        		List<File> xmlFiles = new ArrayList<File>();
        		
        		for (File f:backupFolder.listFiles()){
        			String fName = f.getName();            			
        			int x = fName.contains(".")? fName.lastIndexOf("."):0;
        			
        			if (x > 1) {            				
            			if (fName.substring(x).toLowerCase().equals(".xml") && x-14 > 1) {
            				String dateTime = fName.substring(x-14, x);
        					if (dateTime.matches("\\d{14}")){
        						xmlFiles.add(f);            				
        					}	            				
            			}           
        			}
        		}            		    
    			
    			while (xmlFiles.size() > MAX_BACKUPS) {
    				// Too many backups, delete the oldest files (MAX 10)
    				Date oldestDate = new Date();
    				File fileToDelete = null;
    				
    				for (File f:xmlFiles) {
    					String fName = f.getName();
    					int x = fName.lastIndexOf(".");
    					String dateTime = fName.substring(x-14, x);
    					if (dateTime.matches("\\d{14}")){
    						try {
        						Date temp = dateFormat.parse(dateTime);
        						if (temp.before(oldestDate)) {
        							oldestDate = temp;
        							fileToDelete = f;        							
        						}        				
        					} catch (ParseException e) {
        						e.printStackTrace();
        					}
    					}
    				}
    				
    				try {
						Files.deleteIfExists(fileToDelete.toPath());
						xmlFiles.remove(fileToDelete);
					} catch (IOException e) {
						e.printStackTrace();
					}
    			}
    		}
        }
    }
   
    private void HandleRecordingAborted() {
    	System.out.println("begin HandleRecordingAborted");

    	Platform.runLater(new Runnable() {

            @Override public void run() {
	    		mainApp.showError("Recording error", "The recording was interrupted unexpectedly. Please check manually and restart.");
	    		updateStatus("CAMERA STOPPED RECORDING. RESTART MANUALLY");
            }
        }); 
    	stopRecording();
    }

    private void handleConnectionLost() {
    	System.out.println("begin handleConnectionLost");
    	
    	if (mainApp.getGoPro().getAPIversion() == GoProAPIversion.HERO3 ){
    		displayMessageBox("Connection error", "A connection error occurred. Options: \n  For HERO5 try to connect camera to GoPro app on mobile phone (this keeps it alive). Once done, reconnect to Vino using wifi button. \n  If problem persist, stop ViNO and check for javaw.exe processes running in the background ", false);
  			//updateStatus("LOST CONTACT TO CAMERA. RECONNECT MANUALLY");
	 		setConnectedStatus(false);
	    	stopRecording();

    	} else {
    		// here we need to inform the user and
	 		setConnectedStatus(false);
	 		// Keep trying until we succeed
	 		while (!reconnectToWifi() ) {
	 			; //updateStatus("LOST CONTACT TO CAMERA. TRYING TO RECONNECT. Recording will not be interrupted");
	 		}
 			//updateStatus("Connection re-established");
	 		
    	}
    }

    private void handleUnexpectedConnection() {
    	System.out.println("begin handleUnexpectedConnection");

    	Platform.runLater(new Runnable() {

            @Override public void run() {
//	    		mainApp.showError("Connection reestablished", "Connection to camera was resumed unexpectedly. Check wifi status and camera to verify all is OK. If this continues, restart both ViNO and the GoPro");
	    		updateStatus("Re-established contact with camera");
            }
        }); 
		setConnectedStatus(true);
		if (mainApp.getGoPro().isGoProRecording()) {
			stopRecording();
		}
    }

    private int areStatesOK() throws Exception { // goproCmdMutex protected

    	boolean logicallyConnected = false;
    	boolean physicallyConnected = false;
    	boolean logicallyRecording = false;
    	boolean physicallyRecording = false;
    	int res = 0;

    	goproCmdMutex.lock();
		try {
			// Look for discrepancies between real camera mode and this applications replication of the mode
			// Check the recording states and connection states
			physicallyConnected = isCamConnected();
	    	logicallyConnected = mainApp.getIsConnected();
	    	physicallyRecording = mainApp.getGoPro().isGoProRecording();
			if ( mainApp.getSession() != null ) {
		    	logicallyRecording = mainApp.getSession().getIsRunning();
			}

	    	// Check and handle discrepancies
	    	if (logicallyConnected == physicallyConnected) {
	    		if (logicallyRecording == physicallyRecording) {
	    			
		    	} else {
		    		if (!physicallyRecording) {									
		    			res = 1;				// we think we are recording, but we are not.
		    		} else {													
		    			res = 2;				// we are recording, but we don't think so => How can this happen? 
		    		}
		    	}
	    	} else {
	    		if (!physicallyConnected) {
	    			res = 3;					// we think we are connected, but we are not
	    		} else {
	    			res = 4;					// We are connected, but don't think so.
	    		}
	    	}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("areStatesOK: Exception" );
    	} finally {
    		goproCmdMutex.unlock();
		}
		return res;
    }
    
    private void monitorCamRecordingStatus() { // part of function is goproCmdMutex protected

    	System.out.println("Start background polling of camera recording state");

    	final Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() throws InterruptedException {

            	int currentState = 0;
            	//Timestamp nextKeepAliveTime = new Timestamp(System.currentTimeMillis() + GoProApi._HERO5_KEEPALIVE_POLLINGTIME) ;
            	//Timestamp now = new Timestamp(System.currentTimeMillis()) ;
            	            	
	    		Thread.sleep(GoProApi._STARTUP_PAUSE);  // give it some time to settle in
	    		
		    	while (true){
	    			try {
						Thread.sleep(GoProApi._CAMERA_STATUS_POLLINGTIME);
			    		//System.out.println("Check camera recording state");

						currentState = areStatesOK();
						// Handle incorrect state
						if ( currentState > 0 ) {
							Thread.sleep(2000);   // maybe it's just a glitch in the processing elsewhere. So check again
							if (areStatesOK() == currentState) {
					    		System.out.println("A status inconsistency found " + currentState);
								switch(currentState) {
								case 1:
						    		System.out.println("Handle recording physically aborted" );
									HandleRecordingAborted();
									break;
								case 2:
						    		System.out.println("Handle recording logically aborted" );
									HandleRecordingAborted();
									break;
								case 3:
									System.out.println("Handle physical connection lost" );
									handleConnectionLost();  // this also closes any ongoing recording for HERO3 only
									break;
								case 4:
									System.out.println("Handle logical connection lost" );
									handleUnexpectedConnection();  // this also closes any ongoing recording
									break;
								}
							}
						}
						
	    			} catch (Exception e) {
	    				//e.printStackTrace();
	    				System.out.println("Exception in polling" );
	    			}
		    	}
            }
    	};
        new Thread(task).start();
    }

    public boolean isCamConnected() {

    	BacPacStatus gpstat;
		try {
			//System.out.println("Check camera connected status...");
			gpstat = mainApp.getGoPro().verifyIfGoProIsRecording();  
			//gpstat = mainApp.getGoPro().verifyIfGoProIsPowerOn();
			//System.out.println("CAMERA ON/OFF (BOSSReady) status: " + gpstat.getBOSSReady() + " mainApp.isConnected():" + mainApp.getIsConnected() );
			return (gpstat.getBOSSReady()==1);
		} catch (Exception e) {
			System.out.println("Check camera CONNECTED status...failed");
			//e.printStackTrace();
			return false;
		}
    }

    public void highlightTextFromExternalSearch(String txtToFind, boolean searchForward) throws Exception{

    	int i;
    	int curCaretPos = area.getCaretPosition();
	  
    	try {
			txtToFind = txtToFind.toLowerCase();
	    	//System.out.println("Find result: " + txtToFind);    	 
	    	if (searchForward) {
	    		i = area.getText().toLowerCase().indexOf(txtToFind, curCaretPos);
	    	} else {	
	    		i = area.getText().toLowerCase().lastIndexOf(txtToFind, curCaretPos-txtToFind.length()-1); //NNR: caret is the key
	    	}
	
	    	//System.out.println("i=" + i + " curCaretPos=" + curCaretPos + " rev pos:" + (curCaretPos-txtToFind.length()-1));
	    	if (i > -1) {
	   	    	area.selectRange(i, i+txtToFind.length());  
	   	    	area.requestFocus();
	   	    }
 	  	} catch (Exception e) {
			e.printStackTrace();
 	  	}
    }

    public void setCaretToPosition(int absPosition) {

    	try {
	
	    	if (absPosition > -1) {
	   	    	area.selectRange(absPosition, absPosition);  
	   	    	area.requestFocus();
	   	    }
 	  	} catch (Exception e) {
			e.printStackTrace();
 	  	}
    }

}
