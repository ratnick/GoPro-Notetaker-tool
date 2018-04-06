package org.keo.nt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.gopro.main.GoProApi;
import org.keo.nt.model.Note;
import org.keo.nt.view.CompiledViewController;
import org.keo.nt.view.ImportVideosViewController;
import org.keo.nt.view.MainEditorViewController;
import org.keo.nt.view.MediaViewController;
import org.keo.nt.view.MergeVideosViewController;
import org.keo.nt.view.NoteDetailsViewController;
import org.keo.nt.view.AdvancedSearchViewController;
import org.keo.nt.view.RootLayoutController;
//import org.reactfx.util.Lists;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
//import javafx.stage.Modality;
import javafx.stage.*;
import javafx.util.Pair;

public class MainApp extends Application {
	
	private Stage primaryStage;
	private BorderPane rootLayout;
	private String version;
	private String appTitle;
	private MainEditorViewController mainEditorViewController;
	private GoProApi gopro;
	private States state;
	private Session session;	
	private Note note;
	private BooleanProperty isConnected = new SimpleBooleanProperty(false);
	private BooleanProperty isRecording = new SimpleBooleanProperty(false);
	private File importSource = null;
	private File importDest = null;
	private Boolean mergeAfterImportFlag = false;
	private File mergeDir = null;
	MediaViewController mediaViewController = null;
	private String hdLocation = "";
	
	public MainApp() {
		this.version = "4.0";
		this.appTitle = "ViNo";
		this.note = new Note();
		this.gopro = new GoProApi();
		state = States.PASSIVE;			
	}
	
	public void sethdLocation(String loc) {
		this.hdLocation = loc;
	}
	
	public String gethdLocation() {
		return this.hdLocation;
	}
	
	public void setMergeDir(File dir) {
		this.mergeDir = dir;
	}
	
	public File getMergeDir() {
		return this.mergeDir;
	}
	
	public void setMergeAfterImportFlag(Boolean value) {
		this.mergeAfterImportFlag = value;
	}
	
	public Boolean getMergeAfterImportFlag() {
		return this.mergeAfterImportFlag;
	}
	
	public Note getNote() {
		return this.note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	public MainEditorViewController getMainEditorViewController() {
		return this.mainEditorViewController;
	}

	public GoProApi getGoPro() {
		return this.gopro;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return this.session;
	}

	public States getState() {
		return this.state;
	}

	public void setState(States nextState) {
		this.state = nextState;
	}

	public String getVersion() {
		return this.version;
	}
	
	public void addIsConnectedListener(ChangeListener<? super Boolean> listener) {
		isConnected.addListener(listener);
	}

	public Boolean getIsConnected() {
		return this.isConnected.getValue();
	}

	public void setIsConnected(Boolean isConnected) {
		this.isConnected.set(isConnected);
	}

	public void addIsRecordingListener(ChangeListener<? super Boolean> listener) {
		isConnected.addListener(listener);
	}

	public Boolean getIsRecording() {
		return this.isRecording.getValue();
	}

	public void setIsRecording(Boolean isRecording) {
		this.isRecording.set(isRecording);
	}
	
	public void setImportSource(File src) {
		this.importSource = src;
	}

	public ProgressIndicator getProgressIndicator() {
		return mainEditorViewController.getProgressIndicator();
	}
    
    public Label getProgressStatusLabel() {
    	return mainEditorViewController.getProgressStatusLabel();
    }
    
    public Stage getPrimaryStage() {
		return primaryStage;
	}

	public File getNoteFileFromPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
		String filePath = prefs.get("filePath", null);
		if (filePath != null) {
			return new File(filePath);
		} else {
			return null;
		}
	}

	public void setNoteFileInPrefs(File file) {
		Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
		if (file != null) {
			prefs.put("filePath", file.getPath());
			primaryStage.setTitle(this.appTitle + " - " + file.getName());
		} else {
			prefs.remove("filePath");
			primaryStage.setTitle(this.appTitle);
		}
	}
	
	public void addRecentFilesInPrefs(File file) {
		Preferences prefs = Preferences.userNodeForPackage(MainApp.class);	
		// try to get prefs
		String temp = prefs.get("recentfiles", null);
		if (temp != null && !temp.isEmpty()) {
			// convert string to list
			List<String> recentFiles = new ArrayList<String>(Arrays.asList(temp.split(";")));
			// get path from file
			String filename = file.getPath();
			// check if filename already exists in list
			if (!recentFiles.contains(filename)) {
				
				while (recentFiles.size() >= 3) {
					// remove the oldest fifo
					recentFiles.remove(0);
				}
				
				StringBuffer sb = new StringBuffer();
				
				for ( String i : recentFiles ) {
					sb.append(i+";");
				}				
				
				// concat new file in end of string
				sb.append(file.getPath()+";");
				
				System.out.println(sb.toString());
				
				// put to prefs
				prefs.put("recentfiles", sb.toString());
			}
			
		}else{
			System.out.println("Create a new pref for recent files");
			// no pref, create a new
			prefs.put("recentfiles", file.getPath()+";");
		}
	}

	public File getImportSource() {
		return this.importSource;
	}

	public void setImportDest(File src) {
		this.importDest = src;
	}

	public File getImportDest() {
		return this.importDest;
	}

//	public void setConnectButtonText(String txt) {
//		mainEditorViewController.setConnectButtonText(txt);
//	}

	public File getGoProDrive() {
		String os = System.getProperty("os.name");
		File[] drives=null; 
		if (os.contains("Mac")) {
			//handle mac os stuff
			drives = new File("/Volumes").listFiles();    		
		} else if (os.contains("Windows")) {
			//handle win stuff
			drives = File.listRoots();        	
		} else {
			drives=null;
		}
		for (File drive:drives){
			for (int i=0;i<9;i++){
	    		String goProDrivePath = String.format(drive.getPath() + File.separator + "DCIM" + File.separator + "10%dGOPRO", i);
				File goProDrive = new File(goProDrivePath);		
				if (goProDrive.exists()) {
					return goProDrive;
				}
			}
		}
		return null;
	}
	
	public void setMediaViewController(MediaViewController ctrl) {
    	this.mediaViewController = ctrl;
    }
	
	public MediaViewController getMediaViewController() {
    	return this.mediaViewController;
    }

	public Boolean updateStatus(String txt) {
    	return mainEditorViewController.updateStatus(txt);
    }
    
    public void updateProgress(float value) {
		mainEditorViewController.setProgressValue(value);
	}

	public void print(String line) {
		System.out.println(line);
	}
	
	public void loadNoteDataFromFile(File file) {
		this.getNote().loadFile(file);
		
		if (note.getNoteBody() != null)			
			this.mainEditorViewController.insertEditorText(note.getNoteBody());
		if (note.getQuestions() != null)
			this.mainEditorViewController.insertQuestions(note.getQuestions());
		
        this.setNoteFileInPrefs(file);
        String editorText = getMainEditorViewController().getEditorText();
        this.getMainEditorViewController().markTimestamps(editorText);
        this.getMainEditorViewController().setCaretToPosition(0);
        this.addRecentFilesInPrefs(file);

		}

	public void saveNoteDataToFile(File file) {
		String editorText = getMainEditorViewController().getEditorText();
		List<Pair<String,String>> markdowns = Utils.getMarkDowns(editorText);
		getNote().setNoteAttributes(markdowns);
		getNote().setNoteBody(editorText);
		getNote().setQuestions(getMainEditorViewController().getQuestions());
		
		this.getNote().writeFile(file);
		this.setNoteFileInPrefs(file);
		
		this.addRecentFilesInPrefs(file);
		
	}

	public void showImportVideosView() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/ImportVideosView.fxml"));
			AnchorPane importVideosView = loader.load();
			
			Stage stage = new Stage();
			stage.setTitle("Import videos");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(primaryStage);
	        Scene scene = new Scene(importVideosView);
	        stage.setScene(scene);	        
			
			ImportVideosViewController controller = (ImportVideosViewController)loader.getController();
			controller.setMainApp(this);
			
			stage.showAndWait();
			
			String editorText = getMainEditorViewController().getEditorText();
	        this.getMainEditorViewController().markTimestamps(editorText);
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void showMergeVideosView() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/MergeVideosView.fxml"));
			AnchorPane mergeVideosView = loader.load();
			
			Stage stage = new Stage();
			stage.setTitle("Merge videos");
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(primaryStage);
	        Scene scene = new Scene(mergeVideosView);
	        stage.setScene(scene);	        
			
			MergeVideosViewController controller = (MergeVideosViewController)loader.getController();
			controller.setMainApp(this);
			
			stage.showAndWait();
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void showGetStarted() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/GetStartedView.fxml"));
			AnchorPane getStartedView = loader.load();
			
			Stage getStartedStage = new Stage();
			getStartedStage.setTitle("Note details");
			getStartedStage.initModality(Modality.WINDOW_MODAL);
			getStartedStage.initOwner(primaryStage);
	        Scene scene = new Scene(getStartedView);
	        getStartedStage.setScene(scene);
	
//			NoteDetailsViewController controller = (NoteDetailsViewController)loader.getController();
//			controller.setMainApp(this);
			
			getStartedStage.showAndWait();
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void showNoteDetailsView() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/NoteDetailsView.fxml"));
			AnchorPane noteDetailsView = loader.load();
			
			Stage noteDetailStage = new Stage();
	        noteDetailStage.setTitle("Note details");
	        noteDetailStage.initModality(Modality.WINDOW_MODAL);
	        noteDetailStage.initOwner(primaryStage);
	        Scene scene = new Scene(noteDetailsView);
	        noteDetailStage.setScene(scene);

			
			NoteDetailsViewController controller = (NoteDetailsViewController)loader.getController();
			controller.setMainApp(this);
			
			noteDetailStage.showAndWait();
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void showMainEditorView() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/MainEditorView.fxml"));
			AnchorPane mainEditorView = loader.load();
			
			rootLayout.setCenter(mainEditorView);
			
			mainEditorViewController = loader.getController();
			mainEditorViewController.setMainApp(this);			
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void showCompileView() {
		if (getNoteFileFromPrefs() != null && this.note.getVideoLocation() != null) {	 			
	    		File videoFileDir = new File(this.note.getVideoLocation());
	    		if (videoFileDir.exists()) {
	    			
	    	        try {
	    	        	print("Trying to instantiate fxml");
	    	        	FXMLLoader loader = new FXMLLoader();
		    	        loader.setLocation(MainApp.class.getResource("view/CompiledView.fxml"));
		    	        AnchorPane page = (AnchorPane) loader.load();
	
		    	        Stage compiledStage = new Stage();
		    	        compiledStage.setTitle("Compiled note");
		    	        compiledStage.initModality(Modality.WINDOW_MODAL);
		    	        compiledStage.initOwner(primaryStage);
		    	        compiledStage.setResizable(false);
		    	        Scene scene = new Scene(page);
		    	        compiledStage.setScene(scene);
	
		    	        CompiledViewController controller = loader.getController();
		    	        print("Configuring compile view");
	    				controller.configureView(this,compiledStage);
	    				
	    				print("Configuring succeeded");
	    				compiledStage.show();
	    			} catch (Exception e) {	    				
	    				this.showError("Couldn't compile file", "Please check the note details and try again. Usually this problem is caused by an incorrect path to the video folder.");
	    			}
	    		} else {
	    			this.showError("Couldn't locate directory", "The video directory does not seem to exist. Please check the note details and try again.");
	    		}
		} else {
			this.showError("File error", "Make sure the note is saved and videos are imported. Check the note details for more information.");
		}
	}
	
//	public void showMediaView() {
//		
//        try {
//        	print("Trying to instantiate fxml");
//        	FXMLLoader loader = new FXMLLoader();
//	        loader.setLocation(MainApp.class.getResource("view/MediaView.fxml"));
//	        AnchorPane page = (AnchorPane) loader.load();
//
//	        Stage mediaViewerStage = new Stage();
//	        mediaViewerStage.setTitle("Media Viewer");
//	        mediaViewerStage.initModality(Modality.WINDOW_MODAL);
//	        mediaViewerStage.initOwner(primaryStage);
//	        mediaViewerStage.setResizable(false);
//	        Scene scene = new Scene(page);
//	        mediaViewerStage.setScene(scene);
//
//	        //CompiledViewController controller = loader.getController();
//	        //print("Configuring compile view");
//			//controller.configureView(this,mediaViewerStage);
//			
//			//print("Configuring succeeded");
//			mediaViewerStage.show();
//		} catch (Exception e) {	    				
//			this.showError("Couldn't compile file", "Please check the note details and try again. Usually this problem is caused by an incorrect path to the video folder.");
//		}
//	    		
//	}
	
	public void showError(String header, String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(header);
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	public void showInfo(String title, String header, String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	public Boolean showConfirmation(String title, String header, String message) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(message);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
		    return true;
		} else {
		    return false;
		}
	}
	
	public void maximizeWindow(Boolean value) {
		this.primaryStage.setMaximized(value);
	}
	
	public void initRootLayout() {
		try {
	        FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
	        rootLayout = (BorderPane) loader.load();
	
	        Scene scene = new Scene(rootLayout);
	        //System.out.println("CSS: "+MainApp.class.getResource("../../../resources/default_styles.css"));
	        //System.out.println("CSS: "+MainApp.class.getResource("/Stylesheets/default_styles.css"));
	        String css = getClass().getResource("mainapp.css").toExternalForm();
	        scene.getStylesheets().add(css);
	        
	        primaryStage.setScene(scene);
	        
	        RootLayoutController controller = loader.getController();
	        controller.setMainApp(this);
	        
	        primaryStage.show();
	        
	        scene.getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
	            public void handle(WindowEvent ev) {
	                if (!controller.exitApplication()) {
	                    ev.consume();
	                }
	            }
	        });
	        
	    } catch (IOException e) {
	        this.showError("Couldn't start app", e.getMessage());
	    }
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
	    this.primaryStage.setTitle(this.appTitle + " " + this.version);
	    //this.primaryStage.setMaximized(true);
		
		this.initRootLayout();
		this.showMainEditorView();
		
		File noteFile = this.getNoteFileFromPrefs();
		if (noteFile != null) {
			this.loadNoteDataFromFile(noteFile);
		}
		
	}

	public static void main(String[] args) {
		launch(args);
	}

	public void showAdvancedSearchView() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/AdvancedSearchView.fxml"));
			AnchorPane advancedSearchView = loader.load();
			
			Stage stage = new Stage();
			stage.setTitle("Search");
			stage.initModality(Modality.WINDOW_MODAL);
	        Scene scene = new Scene(advancedSearchView);
	        stage.setScene(scene);	        
			
	        AdvancedSearchViewController controller = (AdvancedSearchViewController)loader.getController();
			controller.setMainApp(this);
			
			stage.show();
			
		} catch (IOException e) {
			this.showError("Couldn't start app", e.getMessage());
		}
	}
	
	public void testme() {
		// put test code here
	}
	

}
