package org.keo.nt.view;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class BrowserViewController {
	
	@FXML private WebView webView;
	
	@FXML private void initialize() {
		
	}
	
	public void loadURL(String url) {
		WebEngine engine = webView.getEngine();
		engine.load(url);
	}
}
