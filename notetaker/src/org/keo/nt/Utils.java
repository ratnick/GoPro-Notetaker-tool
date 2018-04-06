package org.keo.nt;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coremedia.iso.IsoFile;

import javafx.util.Duration;
import javafx.util.Pair;

public class Utils {
	
	public static String TIMESTAMP_REGEX = "(<TIMESTAMP (timecode=\"(\\d{2}:\\d{2}:\\d{2})\") (filename=\"((GOPR|GP\\w{2})\\d{4}.MP4)\")>)";
	
	public static enum StyleType {
		DEFAULT,
    	TIMESTAMP,
    	FONTCOLOR
    }
	
	public static Boolean isTimestampValid(String timestamp) {
		timestamp = timestamp.toUpperCase();
		return timestamp.matches("<<\\d{2}:\\d{2}:\\d{2}\\((GOPR|GP\\w{2})\\d{4}.MP4\\)>>");		
	}
	
	public static String getTimeCode(String timestamp) {
		Pattern p = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
		Matcher m = p.matcher(timestamp);
		System.out.println(m.group());
		return m.group();
	}
	
	public static String getFileName(String timestamp) {
		Pattern p = Pattern.compile("((GOPR|GP\\w{2})\\d{4}.MP4)");
		Matcher m = p.matcher(timestamp);
		int i = 0;
		while (i < m.groupCount()) {
			System.out.println("GROUP " +m.group(i));
			i++;
		}
		return m.group();
	}
	
	public static List<TimeStamp> getTimeStamps(String input) {

		Pattern p = Pattern.compile("<TIMESTAMP(.*?)/>");
		Matcher m = p.matcher(input);

		List<TimeStamp> timestamps = new ArrayList<TimeStamp>();
		while (m.find()) {
			TimeStamp newObj = new TimeStamp(m.group(1));
			newObj.setStart(m.start());
			newObj.setEnd(m.end());
			timestamps.add(newObj);
		}
		
		return timestamps;
	}
	
	public static List<Pair<String,String>> getMarkDowns(String input) {

		Pattern p = Pattern.compile("### (\\w+) (.+)");
		Matcher m = p.matcher(input);

		List<Pair<String,String>> markdowns = new ArrayList<Pair<String,String>>();
		while (m.find()) {
			Pair<String, String> pair = new Pair<String, String>(m.group(1),m.group(2));			
			markdowns.add(pair);			
		}
		
		return markdowns;
	}
	
	public static String updateVideoLocationMarkdown(String input, String videolocation) {
		Pattern p = Pattern.compile("### (\\w+) (.+)");
		Matcher m = p.matcher(input);

		while (m.find()) {
			if (m.group(1).equals("videolocation")) {
				int start = m.start(2);
				int end = m.end(2);
				input = input.substring(0, start) + videolocation + input.substring(end);
			}			
		}
		
		return input;
	}
	
	public static long convertHMStoSec(String hms) {
		hms = hms.trim();
		String[] timeComponents = hms.split(":");
		long secs = Long.parseLong(timeComponents[0])*3600;
		secs += Long.parseLong(timeComponents[1])*60;
		secs += Long.parseLong(timeComponents[2]);
		return secs;
	}
	
	public static long convertHMStoMSec(String hms) {
		return 1000*convertHMStoSec(hms);
	}
	
	public static Map<String, Object> handleChapters(String videopath, String videoname, long delay) {
		
		String prefix = "";
		
		if (!videopath.isEmpty() && videopath.length()>4) {
			prefix = videopath.substring(0, 4);
		}
		
		if (!prefix.equals("http")) {
			Map<String, Object> result = new HashMap<>();
			String filePath = makeVideoPath(videopath, videoname);
			
			if (!(new File(filePath).exists())) {
				return result;
			}
			
			long duration = getDuration(filePath);
			long accumulatedDuration = duration;
			
			if (duration < delay) {
				// find file chapter and adjust delay
				int MAX_CHAPTERS = 10;
				int chapter = 1;
				while (chapter < MAX_CHAPTERS) {
					String nextChapter = String.format("GP%02d", chapter) + videoname.substring(4);	
					String nextChapterPath = makeVideoPath(videopath, nextChapter);
					if (!(new File(nextChapterPath).exists())) {
						break;
					}
					long chapterDuration = getDuration(nextChapterPath);
					long chapterDelayInSecs = delay - accumulatedDuration;
					 
					if (chapterDelayInSecs < chapterDuration) {
						// Found the chapter			
						result.put("delay", new Duration(chapterDelayInSecs*1000));
						result.put("chapter", nextChapter);
						
						break;
					}
					chapter++;
					accumulatedDuration += chapterDuration;
				}
		
			} else {
				result.put("delay", new Duration(delay*1000));
				result.put("chapter", videoname);			
			}	
			return result;
		} else {
			Map<String, Object> result = new HashMap<>();
			
			long duration = 1565; //TODO duration in seconds - value is only valid when correct configuration is sent
			long remainingDuration = delay;
			String chapterName = videoname;
			int chapter = 0;
			
			while (duration < remainingDuration) {
				chapter++;
				chapterName  = String.format("GP%02d", chapter) + videoname.substring(4);					
				remainingDuration = remainingDuration-duration;				
			}
			
			result.put("delay", new Duration(remainingDuration*1000));
			result.put("chapter", chapterName);
			
			return result;
		}
	}
	
	public static String makeVideoPath(String path, String file) {
    	return path + File.separator + file;
    }
    
	public static Long getDuration(String file) {
	    IsoFile isoFile = null;
		try {
			isoFile = new IsoFile(file);
			long duration = isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
		    isoFile.close();
		    return duration;
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return 0L;
	}
	
	public static File getCameraPath() {
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
	
	public static void checkURL(String url) throws UnknownHostException{		
	    try
	    {
	        InetAddress inetAddress = InetAddress.getByName(url);
	        // show the Internet Address as name/address
	        System.out.println(inetAddress.getHostName() + " " + inetAddress.getHostAddress());
	    }
	    catch (UnknownHostException exception)
	    {
	        System.err.println("ERROR: Cannot access '" + url + "'");
	        throw new UnknownHostException();
	    }
	}
}
