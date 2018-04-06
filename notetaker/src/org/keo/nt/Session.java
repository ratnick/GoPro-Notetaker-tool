package org.keo.nt;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Session {
	private Instant startInstant;
	private List<String> timestamps;
	private BooleanProperty isRunning;	
	private String videoName;
	
	public Session() {
		startInstant = Instant.now();
		timestamps = new ArrayList<String>();
		isRunning = new SimpleBooleanProperty();
		isRunning.setValue(false);		
	}
	
	public Session(Instant start) {
		startInstant = start;
		timestamps = new ArrayList<String>();
	}
	
	public void addTimestamp(String timestamp) {
		timestamps.add(timestamp);
	}
	
	public long getSessionDurationInSecs() {
		if (startInstant != null) {
			long duration = Duration.between(startInstant, Instant.now()).toMillis()/1000;
			return duration;
		}
		return 0;
	}
	
	public String getSessionDurationHMS() {
		long durationSecs = getSessionDurationInSecs();
		if (durationSecs>0) {
			long secs = durationSecs % 60;
			long mins = durationSecs / 60;
			long hours = mins / 60;
			mins = mins % 60;
			
			DecimalFormat formatter = new DecimalFormat("00");
			String durationString = formatter.format(hours) + ":" + formatter.format(mins) + ":" + formatter.format(secs);
			
			return durationString;
		}
		return null;
	}  
	
	public long getSecsFromHMS(String hms) {
		hms = hms.trim();
		String[] timeComponents = hms.split(":");
		long secs = Long.parseLong(timeComponents[0]);
		secs += Long.parseLong(timeComponents[1]);
		secs += Long.parseLong(timeComponents[2]);
		return secs;
	}
	
	public void setStartInstant(Instant start) {
		this.startInstant = start;
	}
	
	public Instant getStartInstant() {
		return this.startInstant;
	}
	
	public Boolean getIsRunning() {
        return isRunning.get();
    }

    public void setIsRunning(Boolean isRunning) {
        this.isRunning.set(isRunning);
    }

    public BooleanProperty isRunningProperty() {
        return isRunning;
    }
    
    public void setVideoName(String videoName) {
    	this.videoName = videoName;
    }
    
    public String getVideoName() {
    	return this.videoName;
    }
}
