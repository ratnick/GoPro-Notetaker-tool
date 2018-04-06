package org.keo.nt.view;

import java.io.File;

import org.keo.nt.MainApp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ImportVideosViewController {

	private MainApp mainApp;
	
	private File fileSource = null;
	private File fileDest = null;
	
	@FXML private TextField sourceTextField;
	@FXML private TextField destTextField;
	@FXML private CheckBox mergeCheckBox;
	
	@FXML private void handleSourceBtn() {
		DirectoryChooser chooser = new DirectoryChooser();        		
    	chooser.setTitle("Source directory");		
		File selectedDirectory = chooser.showDialog(mainApp.getPrimaryStage());
		if (selectedDirectory != null) {
			sourceTextField.setText(selectedDirectory.getPath().toString());
			fileSource = selectedDirectory;
		}
	}
	
	@FXML private void handleDestBtn() {
		DirectoryChooser chooser = new DirectoryChooser();        		
    	chooser.setTitle("Destination directory");		
		File selectedDirectory = chooser.showDialog(mainApp.getPrimaryStage());
		if (selectedDirectory != null) {
			destTextField.setText(selectedDirectory.getPath().toString());
			fileDest = selectedDirectory;
		}
	}
	
	@FXML private void handleOkBtn(ActionEvent event) {
		
		fileSource = new File(sourceTextField.getText());
		fileDest = new File(destTextField.getText());
		
		if (fileSource != null && fileSource.exists())
			mainApp.setImportSource(fileSource);
		if (fileDest != null && fileDest.exists())
			mainApp.setImportDest(fileDest);
		
		mainApp.setMergeAfterImportFlag(mergeCheckBox.isSelected());		
		
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}
		
	@FXML private void handleCancelBtn(ActionEvent event) {
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}
	
	public void setMainApp(MainApp mainApp){
		this.mainApp = mainApp;
		System.out.println("Import");
		File gpDrive = mainApp.getGoProDrive();
		if (gpDrive != null) {
			sourceTextField.setText(gpDrive.getPath());
			System.out.println("Import "+sourceTextField.getText());
		}
		
		File noteFile = mainApp.getNoteFileFromPrefs();
		if (noteFile != null) {			
			String absolutePath = noteFile.getAbsolutePath();
			String filePath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
			destTextField.setText(filePath);
			System.out.println("Import "+destTextField.getText());
		}
	}
}
