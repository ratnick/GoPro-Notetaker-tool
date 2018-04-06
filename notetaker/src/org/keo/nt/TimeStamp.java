package org.keo.nt;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeStamp {
	
	private Optional<String> timestamp;
	private Optional<String> timecode;
	private Optional<String> filename;
	private Optional<Number> start;
	private Optional<Number> end;
	
	public TimeStamp(String timestamp) {
		this.timestamp = Optional.ofNullable(timestamp.trim());
		// decode timestamp
		Pattern p = Pattern.compile("timecode=\"(.*?)\" filename=\"(.*?)\"");
		Matcher m = p.matcher(this.timestamp.orElse(""));

		if (m.find()) {
			//System.out.println("timestamp " + m.group(1) + " filename " + m.group(2));
			timecode = Optional.ofNullable(m.group(1));
			filename = Optional.ofNullable(m.group(2));
		}
	}
	
	public TimeStamp(String timecode, String filename) {
		this.timecode = Optional.ofNullable(timecode);
		this.filename = Optional.ofNullable(filename);		 
	}
	
	public Boolean isValid(String path) {
		File videofile = new File(Utils.makeVideoPath(path, filename.get()));
		if (videofile.exists()) 
			return true;
		else
			return false;
	}
	
	public String getTimecode() {
		return timecode.orElse("");
	}
	
	public String getFilename() {
		return filename.orElse("");
	}

	public String getTimestamp() {
		return timestamp.orElse("");
	}
	
	public int getStart() {
		return this.start.get().intValue();
	}
	
	public void setStart(int start) {
		this.start = Optional.ofNullable(start);
	}
	
	public int getEnd() {
		return this.end.get().intValue();
	}
	
	public void setEnd(int end) {
		this.end = Optional.ofNullable(end);
	}
}
