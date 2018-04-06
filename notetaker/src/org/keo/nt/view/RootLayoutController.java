package org.keo.nt.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.keo.nt.MainApp;
import org.keo.nt.Utils;
import org.keo.nt.model.Note;
//import org.keo.nt.utils.WifiOperations;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;


public class RootLayoutController implements PreferenceChangeListener{
	
    private MainApp mainApp;
    
    @FXML private Menu menuGoPro;
    @FXML private Menu recent;
    
    @FXML private void initialize() {
    	this.menuGoPro.setDisable(true);
    	
    	Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
    	prefs.addPreferenceChangeListener(this);
    	
    	this.updateRecentFilesMenu();
    }
    
    
    public Boolean exitApplication() {
    	System.out.println("EXIT");
    	doSave();
    	return true;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        
        mainApp.addIsConnectedListener(new ChangeListener<Object>() {
			
        	public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue){
				Boolean isConnected = (Boolean)newValue;
				if (isConnected) {
					disableMenuGoPro(false);
				} else {
					disableMenuGoPro(true);
				}
			}
		});
    }
    
    private void disableMenuGoPro(Boolean enable) {
    	this.menuGoPro.setDisable(enable);
    }
    
    @FXML
    private void handleNew() {
    	mainApp.setNote(new Note());
    	mainApp.setNoteFileInPrefs(null);
    	mainApp.getMainEditorViewController().deleteEditorText();
    	mainApp.getMainEditorViewController().deleteQuestionsText();    	
    	mainApp.showNoteDetailsView();
    	doSave();
    }

    /**
     * Opens a FileChooser to let the user select an address book to load.
     */
    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());

        if (file != null) {
        	mainApp.loadNoteDataFromFile(file);           	
        }
    }
    
    /**
     * Saves the file to the person file that is currently open. If there is no
     * open file, the "save as" dialog is shown.
     */
    @FXML
    private void handleSave() {
    	doSave();
    }
    	
    private void doSave() {
    	
		File noteFile = this.mainApp.getNoteFileFromPrefs();
        if (noteFile != null) {
        	
//        	if ( !mainApp.getNote().isReadyToSave() ) {
//            	mainApp.showNoteDetailsView();
//        	} 
        	
        	mainApp.saveNoteDataToFile(noteFile);
        	mainApp.updateStatus("Saved!");
        	
        } else {
            handleSaveAs();
        }    	
    }

    /**
     * Opens a FileChooser to let the user select a file to save to.
     */
    @FXML
    private void handleSaveAs() {
    	    	
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(mainApp.getNote().getRespondentId()+"_"+mainApp.getNote().getFirstName()+"_"+mainApp.getNote().getLastName());

        File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());

        if (file != null) {
            // Make sure it has the correct extension
            if (!file.getPath().endsWith(".xml")) {
                file = new File(file.getPath() + ".xml");
            }
//            if ( !mainApp.getNote().isReadyToSave() ) {
//            	mainApp.showNoteDetailsView();
//        	} 
        	mainApp.saveNoteDataToFile(file); 
        	mainApp.updateStatus("Saved!");
        }
    }
    
    @FXML private void handleImportVideoFiles() {
    	
    	mainApp.showImportVideosView();
    	
    	if (mainApp.getImportSource() != null && mainApp.getImportDest() != null) {    		
    		startCopyTask(mainApp.getImportSource(), mainApp.getImportDest());
    		mainApp.getNote().setVideoLocation(mainApp.getImportDest().getAbsolutePath());
    		String updatedText = Utils.updateVideoLocationMarkdown(mainApp.getMainEditorViewController().getEditorText(), mainApp.getNote().getVideoLocation());
        	mainApp.getMainEditorViewController().insertEditorText(updatedText);
    	}
    	
    }
    
    @FXML private void handleMergeVideos() {
    	
    	mainApp.showMergeVideosView();
    	
    	if (mainApp.getMergeAfterImportFlag()) {
    		File dir = mainApp.getMergeDir();
    		if (dir != null && dir.exists()) {
    			this.postImportParsing(dir);
    		}    		
    	}
    }
    
    @FXML private void handleMarkTimestamps() {
    	mainApp.getMainEditorViewController().markTimestamps(mainApp.getMainEditorViewController().getEditorText());
    }
    
    private void startCopyTask(File source, File dest) {
    	
    	final Task<Boolean> task = new Task<Boolean>() {
            @SuppressWarnings("resource")
			@Override protected Boolean call() throws InterruptedException {
            	
            	Boolean status = false;
            	
            	File[] files = source.listFiles(new FilenameFilter() {
            	    public boolean accept(File dir, String name) {
            	        return name.toLowerCase().endsWith(".mp4");
            	    }
            	});
            	
            	updateMessage("Start copying...");
        		updateProgress(-1,0);
        		
            	int count = 1;
            	for (File file: files) {    		
            	
            		updateMessage("Copying "+count+" of "+files.length);
            		
            		String copyToFile = dest + File.separator + file.getName(); 
            		File destFile = new File(copyToFile);
            		
            		FileChannel inputChannel = null;    
    		        FileChannel outputChannel = null;
    		        
    		        try {
    		            
    		            inputChannel = new FileInputStream(file).getChannel();    
    		            outputChannel = new FileOutputStream(destFile).getChannel();    		            
    		            
    		            int attempts = 0;
    		            long expectedSize = inputChannel.size();
    		            Boolean success = false;
    		            
    		            do {
    		            	
    		            	// ******************************************************************* //
    		            	long position = 0, size = inputChannel.size();
    		            	    		            	
    		            	updateProgress(0,size);
    		            	
    		            	while (position < size) {
    		            		 position += outputChannel.transferFrom(inputChannel, position, 1024*1000);
    		            		 updateProgress(position,size);    		            		 
    		            		}
    		            	
    		            	updateProgress(size,size);
    		            	// ******************************************************************* //
    		            	
    		            	attempts++;
    		            	if (expectedSize == position) {
    		            		success = true;
    		            	}
    		            }while(!success && attempts < 5);
    		            
    		            if (!success) {
    		            	//failed to copy
    		            	updateMessage("Failed to copy "+file.getName());
    		            	Thread.sleep(2000);
    		            }
    		            
    		        } catch (IOException e) {
        				mainApp.showError("Couldn't copy files", e.getMessage());
        			} 
    		        finally {
        				try {
							inputChannel.close();
							outputChannel.close();
						} catch (IOException e) {
							System.out.println("Couldn't close file channels");
						}
        				
        			}
        		    
            		count++;
            	}
            	updateMessage("Copied all");
              
              return status;
            }
          };
          
          mainApp.getProgressStatusLabel().textProperty().bind(task.messageProperty());
          mainApp.getProgressIndicator().progressProperty().bind(task.progressProperty());
          
          task.stateProperty().addListener(new ChangeListener<Worker.State>() {
              @Override public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {                                  
                  mainApp.getProgressStatusLabel().textProperty().unbind();
                  mainApp.getProgressIndicator().progressProperty().unbind();
                  if (mainApp.getMergeAfterImportFlag()) {
                	  System.out.println("Merging...");
                	  postImportParsing(dest);  
                  }                  
                  mainApp.showInfo("Import Done", "All files are imported", "All the videos are now imported. Chaptered videos are being merged into the base file and this process may stil be running for a while, please check the status bar.");
                }
              }
            });
          
          new Thread(task).start();
    }
    
    private void postImportParsing(File dir) {
    	
    	List<File> mp4Files = Arrays.asList(dir.listFiles(new FilenameFilter() {
    	    public boolean accept(File dir, String name) {    	    	
    	        return name.toLowerCase().endsWith(".mp4");
    	    }
    	}));
    	
    	List<File> chapteredFiles = new ArrayList<File>();    	
    	for (File file : mp4Files) {
    		String[] tokens = file.getName().split(":");
    		if (tokens[0].substring(2, 4).equals("PR")) {
    			chapteredFiles.add(file);
    		}    		  	
    	}
    	
    	for (File file1 : chapteredFiles) {
    		String id = file1.getName().substring(4,8);
    		List<File> fileChapters = new ArrayList<File>();
    		fileChapters.add(file1);
    		for (File file2 : mp4Files) {
    			if (file2.getName().substring(4, 8).equals(id) && !file2.getName().substring(2, 4).equals("PR")) {
    				fileChapters.add(file2);
    			}
    		}
    		if (fileChapters.size() > 1) {
    			// Merge files
    			System.out.println("Chaptered file found: "+file1.getName()+"\nChapters: "+fileChapters.size());
    			for (File file : fileChapters) {
    				System.out.println(file.getAbsolutePath());      				
    			}
    			mainApp.setMergeAfterImportFlag(false);
    			mergeFiles(fileChapters);
    		}
    	}  	
    }
    
    private void mergeFiles(List<File> files) {
    	
    	final Task<Boolean> task = new Task<Boolean>() {
            @SuppressWarnings("resource")
			@Override protected Boolean call() throws InterruptedException {
            	
            	Boolean status = false;
            	            	
            	try {
            		            		
            		updateMessage("Loading chapters..");
        			        			
        			List<Movie> movies = new ArrayList<Movie>();
        			updateProgress(0,files.size());
        			int count = 0;
        			
        			for (File file : files) {
        				updateProgress(count,files.size());
        				updateMessage("Preparing: "+file.getName()); 
        				Movie m = MovieCreator.build(file.getAbsolutePath());
        				movies.add(m);
        				count++;
        			}						
        	
        			updateProgress(1,1);
        			
        			updateMessage("Processing tracks..");
        	        List<Track> videoTracks = new LinkedList<Track>();
        	        List<Track> audioTracks = new LinkedList<Track>();        	        
         	        count = 0;
         	        
        	        for (Movie movie: movies) {
        	        	updateProgress(count,files.size());        				        				
        	            for (Track track : movie.getTracks()) {        	            
        	                if (track.getHandler().equals("soun")) 
        	                    audioTracks.add(track);                
        	                if (track.getHandler().equals("vide"))
        	                    videoTracks.add(track);
        	            }
        	            count++;
        	        }
        	
        	        updateProgress(1,1);
        	        
        	        updateMessage("Merging tracks..");
        	        updateProgress(-1,1);
        	        Movie result = new Movie();
        	        if (videoTracks.size() > 0)
        	            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        	
        	        if (audioTracks.size() > 0) 
        	            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        	        
        	        
        	                	        
        	        String outputFile = files.get(0).getAbsolutePath();
        	        updateMessage("Creating backup of "+files.get(0).getName());        	        
        	                	        
        	        String backupFile = outputFile + ".BACKUP";
        	        File backup = new File(backupFile);
        	        if (backup.exists()) backup.delete();
        	        
        	        {
        	        	FileChannel inputChannel = null;    
        		        FileChannel outputChannel = null;
        		        
        		        try {
        		            
        		            inputChannel = new FileInputStream(outputFile).getChannel();    
        		            outputChannel = new FileOutputStream(backupFile).getChannel();    		            
        		            
        		            int attempts = 0;
        		            long expectedSize = inputChannel.size();
        		            Boolean success = false;
        		            
        		            do {
        		            	
        		            	// ******************************************************************* //
        		            	long position = 0, size = inputChannel.size();
        		            	    		            	
        		            	updateProgress(0,size);
        		            	
        		            	while (position < size) {
        		            		 position += outputChannel.transferFrom(inputChannel, position, 1024*1000);
        		            		 updateProgress(position,size);    		            		 
        		            		}
        		            	
        		            	updateProgress(size,size);
        		            	// ******************************************************************* //
        		            	
        		            	if (expectedSize == position) {
        		            		success = true;
        		            		files.get(0).delete();
        		            	}
        		            	
        		            }while(!success && attempts < 5);
        		            
        		            if (!success) {
        		            	//failed to copy
        		            	updateMessage("Failed to create backup");
        		            	Thread.sleep(2000);
        		            }
        		            
        		        } catch (IOException e) {
            				mainApp.showError("Couldn't backup file", e.getMessage());
            			} 
        		        finally {
            				try {
    							inputChannel.close();
    							outputChannel.close();
    						} catch (IOException e) {
    							System.out.println("Couldn't close file channels");
    						}
            				
            			}
        		        
        	        }
        	                	        
        	        updateMessage("Building video (takes a long time...)");
        	        updateProgress(0,4);
        	        
        	        Container out = new DefaultMp4Builder().build(result);
        	        {
        	        	
        	        	updateMessage("Preparing file");
            	        updateProgress(1,4);
	        	        FileChannel fc = new RandomAccessFile(String.format(outputFile), "rw").getChannel();
	        	        updateMessage("Writing to disk (takes a long time...)");
	        	        updateProgress(3,4);	        	      
	        	        out.writeContainer(fc);
	        	        updateMessage("Merged: "+outputFile);
	        	        updateProgress(0,4);
	        	        fc.close();
        	        }        	        
        	        
        	        updateMessage("Success!");
        	        updateProgress(1,1);
        	        
        		} catch (IOException e) {
        			System.out.println("Merge exception!");
        			updateMessage("Video merge failed!");
        		}
            	
              return status;
            }
          };
          
          mainApp.getProgressStatusLabel().textProperty().bind(task.messageProperty());
          mainApp.getProgressIndicator().progressProperty().bind(task.progressProperty());
          
          task.stateProperty().addListener(new ChangeListener<Worker.State>() {
              @Override public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                	System.out.println("Ended task");   
                	mainApp.getProgressStatusLabel().textProperty().unbind();
	            	mainApp.getProgressIndicator().progressProperty().unbind();
	            	mainApp.showInfo("Merge Done", "Chaptered files are merged", "The chaptered video sequences have been merged into the base recording.");
                }
              }
            });
          
          new Thread(task).start();
		
	}	

	@FXML private void handleFind() {
    	mainApp.showAdvancedSearchView();
	}
    
    @FXML private void handleEditNoteDetails() {
    	mainApp.showNoteDetailsView();
    }
    
    @FXML private void handleCompile() {
    	handleSave();
    	mainApp.showCompileView();
    }

    @FXML private void handleGetStarted() {
    	mainApp.showGetStarted();
    }
    /**
     * Opens an about dialog.
     */
    @FXML
    private void handleAbout() {
    	mainApp.showInfo("NoteTaker", "About NoteTaker v." + mainApp.getVersion(), "\nA note taking tool that connects to a GoPro camera (version 3-6) and lets you create interactive timestamps directly in your note.");       
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
    	Alert alert = new Alert(AlertType.CONFIRMATION);
    	alert.setTitle("Save");
    	alert.setHeaderText("Do you want to save before closing?");
    	alert.setContentText("It is always a good idea to save before closing.");

    	ButtonType saveExit = new ButtonType("Save & Exit");
    	ButtonType justExit = new ButtonType("Just exit");    	
    	ButtonType cancelExit = new ButtonType("Cancel");

    	alert.getButtonTypes().setAll(saveExit, justExit, cancelExit);

    	Optional<ButtonType> result = alert.showAndWait();
    	if (result.get() == saveExit){
    		handleSave();
    		System.exit(0);
    	} else if (result.get() == justExit) {
    		System.exit(0);    	
    	} else {
    		alert.close();
    	}
    }    
    
    @FXML private void handleTurnOn() {
    	try {
			mainApp.getGoPro().powerOn();
		} catch (Exception e) {
			mainApp.showError("Couldn't turn on camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleTurnOff() {
    	try {
			mainApp.getGoPro().powerOff();
		} catch (Exception e) {
			mainApp.showError("Couldn't turn off camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleStartCamera() {
    	try {
    		mainApp.getMainEditorViewController().doStartStopCam();
			//mainApp.getGoPro().powerOnAndStartRecord();
		} catch (Exception e) {
			mainApp.showError("Couldn't start camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleStopCamera() {
    	try {
    		mainApp.getMainEditorViewController().doStartStopCam();
			//mainApp.getGoPro().stopRecord();
		} catch (Exception e) {
			mainApp.showError("Couldn't stop camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleBeepOff() {
    	try {
			mainApp.getGoPro().setBeep(0);
		} catch (Exception e) {
			mainApp.showError("Couldn't reach camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
	@FXML private void handleBeep70() {
		try {
			mainApp.getGoPro().setBeep(1);
		} catch (Exception e) {
			mainApp.showError("Couldn't reach camera", e.getMessage());
			mainApp.setIsConnected(false);
		}	
	}
	
	@FXML private void handleBeep100() {
		try {
			mainApp.getGoPro().setBeep(2);
		} catch (Exception e) {
			mainApp.showError("Couldn't reach camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
	}
    
    @FXML private void handleConfiguration() {
    	try {
			mainApp.getGoPro().sendConfiguration();
		} catch (Exception e) {
			mainApp.showError("Couldn't configure camera", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleDeleteAll() {
    	try {
    		if(mainApp.showConfirmation("Delete all", "Please confirm to delete all files", "Are you really sure that you want to delete all the files?")) {
    			mainApp.getGoPro().deleteAll();
    		}			
		} catch (Exception e) {
			mainApp.showError("Couldn't delete files", e.getMessage());
			mainApp.setIsConnected(false);
		}
    }
    
    @FXML private void handleMaximizeWindow() {
    	mainApp.maximizeWindow(true);
    }

    @FXML private void handleTest() {
    	mainApp.testme();
    }    
    
    
	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		
		String key = evt.getKey();
	    //String val = evt.getNewValue();
	    if (key.equals("recentfiles")) {	    	
	    	// call update recent files menu
	    	this.updateRecentFilesMenu();
	    } 
	}
	
	private void updateRecentFilesMenu() {
		// clear menu items
    	recent.getItems().clear();
    	// get recent files from prefs
    	Preferences prefs = Preferences.userNodeForPackage(MainApp.class);				
		String temp = prefs.get("recentfiles", null);
		if (temp != null && !temp.isEmpty()) {
			// insert recent files in menu items
			ArrayList<String> recentFiles = new ArrayList<String>(Arrays.asList(temp.split(";")));
			//iterate list in reverse order
			ListIterator<String> iter = recentFiles.listIterator(recentFiles.size());
			while(iter.hasPrevious()) {
		    	String s = iter.previous();
				MenuItem mi = new MenuItem(s);				
				mi.setOnAction(new EventHandler<ActionEvent>() {
				    @Override public void handle(ActionEvent e) {	
				        mainApp.loadNoteDataFromFile(new File(s));
				    }
				});
				recent.getItems().add(mi);
			}
		}	    	
    	System.out.println("CHANGED:");
	}
}
