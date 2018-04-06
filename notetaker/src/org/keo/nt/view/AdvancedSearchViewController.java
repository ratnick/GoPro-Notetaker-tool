package org.keo.nt.view;
 
import org.keo.nt.MainApp;
import org.keo.nt.view.MainEditorViewController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
 
public class AdvancedSearchViewController {
	
	private MainApp mainApp;
	@FXML private TextField searchTextField;

    @FXML private void handleSearchBtn(ActionEvent e) {
    	boolean searchForward = e.getSource().toString().contains("Forward");
    	//System.out.println("you search for: " + searchTextField.getText());
		if (mainApp.getMainEditorViewController() != null) {
			try {
				mainApp.getMainEditorViewController().highlightTextFromExternalSearch(searchTextField.getText(), searchForward);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
    }
	
	@FXML private void handleCancelBtn(ActionEvent event) {
		Node source = (Node)event.getSource();
		Stage stage = (Stage)source.getScene().getWindow();
		stage.close();
	}

	public void setMainApp(MainApp mainApp){
		this.mainApp = mainApp;
		System.out.println("Search window");
		//searchTextField.setText("enter text");		
	}

}