package org.keo.nt.view;

import java.io.File;

import org.keo.nt.MainApp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MergeVideosViewController {
	
	private File mergeDirectory;
	private MainApp mainApp;
	
	@FXML private TextField locationTextField;
	
	@FXML private void handleOkBtn(ActionEvent event) {
		
		mergeDirectory = new File(locationTextField.getText());		
		
		if (mergeDirectory != null && mergeDirectory.exists()) {
			mainApp.setMergeAfterImportFlag(true);
			mainApp.setMergeDir(mergeDirectory);
		}
		
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}
		
	@FXML private void handleCancelBtn(ActionEvent event) {
		
		mainApp.setMergeAfterImportFlag(false);
		
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}
	
	@FXML private void handleFolderBtn() {
		DirectoryChooser chooser = new DirectoryChooser();        		
    	chooser.setTitle("Video files directory");		
		File selectedDirectory = chooser.showDialog(mainApp.getPrimaryStage());
		if (selectedDirectory != null) {
			locationTextField.setText(selectedDirectory.getPath().toString());			
		}
	}
	
	public void setMainApp(MainApp mainApp){
		this.mainApp = mainApp;
		
	}
}
