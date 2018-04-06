package org.keo.nt.view;

import java.io.File;

import org.keo.nt.MainApp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class NoteDetailsViewController {

	private MainApp mainApp;
	
	@FXML private TextField firstName;
	@FXML private TextField lastName;
	@FXML private TextField respondentId;
	@FXML private TextField noteTaker;
	@FXML private TextField videoLocation;
	
	@FXML private ImageView firstNameCheck;
	@FXML private ImageView lastNameCheck;
	@FXML private ImageView respondentIdCheck;
	@FXML private ImageView noteTakerCheck;
	
	@FXML private void handleKeyEvent(KeyEvent event) {
		//verifyInputs();
	}	
	
	@FXML private void handleOpenDir(ActionEvent event) {
		openDirChooser();
	}	
	
	@SuppressWarnings("unused")
	@FXML private void handleSave(ActionEvent event) {
		
		//if (verifyInputs()) {
		if(true){
			System.out.println("Saving note details");
			//Save to note
			mainApp.getNote().setFirstName(firstName.getText());
			mainApp.getNote().setLastName(lastName.getText());
			//Integer rId = Integer.parseInt(respondentId.getText());
			mainApp.getNote().setRespondentId(respondentId.getText());
			mainApp.getNote().setNoteTaker(noteTaker.getText());
			if (videoLocation.getText() != null)
				mainApp.getNote().setVideoLocation(videoLocation.getText());			
			
			Node source = (Node)event.getSource();
			Stage stage = (Stage)source.getScene().getWindow();
			stage.close();
		} else {
			mainApp.showError("Incorrect inputs", "Can't save the details. Please enter the details correctly.");
		}
	}
	
	@FXML private void handleCancel(ActionEvent event) {
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}
	
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
		
		if (mainApp.getNote().getFirstName() != null)
			firstName.setText(mainApp.getNote().getFirstName());
		if (mainApp.getNote().getLastName() != null)
			lastName.setText(mainApp.getNote().getLastName());
		if (mainApp.getNote().getRespondentId() != null)
			respondentId.setText(""+mainApp.getNote().getRespondentId());
		if (mainApp.getNote().getNoteTaker() != null)
			noteTaker.setText(mainApp.getNote().getNoteTaker());
		if (mainApp.getNote().getVideoLocation() != null)
			videoLocation.setText(mainApp.getNote().getVideoLocation());
		else
			videoLocation.setText(System.getProperty("user.dir"));
		
	}
	
	public Boolean verifyInputs() {
				
		if (firstName.getText().matches("[a-zA-Zæøå0-9 ]+")) {
			firstNameCheck.setOpacity(1.0);
		}
		else {
			firstNameCheck.setOpacity(0);
		}
		
		if (lastName.getText().matches("[a-zA-Zæøå0-9 ]+")) {
			lastNameCheck.setOpacity(1.0);
		} else {
			lastNameCheck.setOpacity(0);
		}
			
		
		if (respondentId.getText().matches("\\d+")) {
			respondentIdCheck.setOpacity(1.0);
		} else {
			respondentIdCheck.setOpacity(0);
		}
		
		if (noteTaker.getText().matches("[a-zA-Zæøå0-9 ]+")) {
			noteTakerCheck.setOpacity(1.0);
		} else {
			noteTakerCheck.setOpacity(0);
		}
		
		if (firstNameCheck.getOpacity() == 1.0 && lastNameCheck.getOpacity() == 1.0 && respondentIdCheck.getOpacity() == 1.0 && noteTakerCheck.getOpacity() == 1.0) {
			return true;
		} else {
			return false;
		}
	}
	
	public void openDirChooser() {
		
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Video file location");		
		File selectedDirectory = chooser.showDialog(mainApp.getPrimaryStage());
		if (selectedDirectory != null) {
			String dirPath = selectedDirectory.getPath().toString();		
			videoLocation.setText(dirPath);	
		}
	}
	
}
