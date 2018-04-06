package org.keo.nt.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WifiOperations {

	private static String wifiSSID = "";
	private static String wifiInterfaceName;

	private static boolean IsWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}
	
	private static String getValue(String s) {
		Pattern p = Pattern.compile("(: )(.*)");
		Matcher m = p.matcher(s);
		if (m.find()) {
			return m.group(2);
		} else {
			return "";
		}
	}
	
	public static String GetWifiInfo() {
		Process p;
		String line;
		int exitCode;
		
		try {
			System.out.println("GetWifiInfo ");
			if (IsWindows()) {
				p = Runtime.getRuntime().exec(String.format("netsh wlan show interface"));
			} else {
			    p = Runtime.getRuntime().exec(String.format("netsh wlan show interface"));  //TODO: How to do this on a MAC?
			}
		    BufferedReader input = new BufferedReader (new InputStreamReader(p.getInputStream()));
		    
		    for (line = input.readLine(); line != null; line = input.readLine()) {
				//System.out.println("GetWifiInfo: \n" + line);
		    	if (line.contains("    Name") ) { wifiInterfaceName = getValue(line); }
		    	if (line.contains("    SSID") ) { wifiSSID  = getValue(line); }
		    }
		    //			line = input.readLine();
			input.close();
			exitCode = p.waitFor();
			p.destroy();
		} catch (Exception e) {
			// this would be unexpected
			System.out.println("GetWifiInfo fails " + e.getMessage());
			return "GetWifiInfo fails " + e.getMessage();
		}
		return wifiSSID;
	}
	
	public static String ConnectToGoProWifi(String ssid) {
		Process p;
		String line;
		int exitCode;
		
		if (!ssid.isEmpty())  {
			wifiSSID = ssid;
		}

		try {
			System.out.println("Try ConnectToGoProWifi " + wifiSSID);
			if (IsWindows()) {
				p = Runtime.getRuntime().exec(String.format("netsh wlan disconnect interface=%s", wifiInterfaceName));
				Thread.sleep(4000); // Allow wifi to settle
				p = Runtime.getRuntime().exec(String.format("netsh wlan connect name=%s", wifiSSID));
				Thread.sleep(4000); // Allow wifi to settle
			} else {
				//TODO: IMPORTANT: How to do this on a Mac
				p = Runtime.getRuntime().exec(String.format("netsh wlan disconnect interface=%s", wifiInterfaceName));
				Thread.sleep(4000); // Allow wifi to settle
				p = Runtime.getRuntime().exec(String.format("netsh wlan connect name=%s", wifiSSID));
				Thread.sleep(4000); // Allow wifi to settle
			}
		    BufferedReader input = new BufferedReader (new InputStreamReader(p.getInputStream()));
			line = input.readLine();
			input.close();
			exitCode = p.waitFor();
			Thread.sleep(10000); // Allow wifi to settle
			p.destroy();
		} catch (Exception e) {
			// this would be unexpected
			System.out.println("ConnectToGoProWifi fails " + e.getMessage());
			return "ConnectToGoProWifi fails " + e.getMessage();
		}
		if (exitCode != 0) {
			// this would be expected if gopro wireless not turned on or seen by PC
			if (line.contains("The network specified by profile \"" + wifiSSID + "\" is not available to connect")) {
				return "Try to enable wifi on GoPro and refresh network list on computer";
			}
		}
		return "";  //"" = success
	}
}

