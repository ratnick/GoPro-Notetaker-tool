package org.gopro.core;

import java.net.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.gopro.core.model.BacPacStatus;
import org.gopro.core.model.BackPack;
import org.gopro.core.model.CamFields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoProHelper {

	public static final boolean LOGGING_ENABLED = false;
//	private final HttpClient mClient = newInstance();
	public GoProAPIconstants activeGoProAPI = new GoProAPIconstants(GoProAPIversion.HERO56);

	public enum GoProAPIversion {HERO3, HERO4Session, HERO56, UNDETERMINED};

	public class GoProAPIconstants {
		private GoProAPIversion goProAPIversion;
		private String ipAddress;
		private Integer port;
		private String SSID;
		private String password;
		private String httpAddr1;
		private String httpAddr2;
		private String knownMACaddresses;
		private boolean goproHasBeenConnectedOnce;
		
		GoProAPIconstants(GoProAPIversion APIver) {
			this.goProAPIversion = APIver;
			this.ipAddress = "10.5.5.9"; // applies to all versions of gopro API
			this.goproHasBeenConnectedOnce = false;

			switch (APIver) {
				case HERO3:
					this.port = 80;
					this.password = "uxccuxcc";
					this.httpAddr1 = "http://" + this.ipAddress + ":" + this.port;
					this.httpAddr2 = "?t=" + this.password;
					break;
				case HERO4Session:
					this.port = 9;
					this.password = "rasmussen";
					this.knownMACaddresses = "F6:DD:9E:13:F5:1F";  // TODO move MAC addresses to the UI or a config file. Only needed for WOL
					this.httpAddr1 = "http://" + this.ipAddress + ":" + this.port;
					this.httpAddr2 = "";
					break;
				case HERO56:
					this.port = 9;
					this.password = "";                            // No password needed. Only used during wifi connection setup on PC/Mac
					this.httpAddr1 = "http://" + this.ipAddress;   // No port needed
					this.httpAddr2 = "";                           //https://github.com/KonradIT/goprowifihack/blob/master/HERO4/WifiCommands.md 
					this.knownMACaddresses = ""; //"F6:DD:9E:13:F5:1F";  // TODO move MAC addresses to the UI or a config file. Only needed for WOL (i.e. GoPro Session 4)
					break;
				default:
					break;
			}
		}
	}

	public GoProAPIversion getAPIversion() {
		return activeGoProAPI.goProAPIversion;
	}

	public void setAPIversion(GoProAPIversion v) {
		activeGoProAPI.goProAPIversion = v;
	}

	public void setSSID(String ssid) {
		activeGoProAPI.SSID = ssid;
	}
	
	public String getSSID() {
		return activeGoProAPI.SSID;
	}
	
	public void setGoproHasBeenConnectedOnce(boolean val) {
		activeGoProAPI.goproHasBeenConnectedOnce = val;
	}
	
	public boolean getGoproHasBeenConnectedOnce() {
		return activeGoProAPI.goproHasBeenConnectedOnce;
	}
	
	public GoProHelper() {

	}

	public GoProHelper(Integer port, String password) {
		this();
	}

	private boolean passFail(byte[] paramArrayOfByte) {
		boolean bool = false;
		if (paramArrayOfByte != null) {
			int i = paramArrayOfByte.length;
			bool = false;
			if (i > 0) {
				int j = paramArrayOfByte[0];
				bool = false;
				if (j == 0)
					bool = true;
			}
		}
		return bool;
	}

	public boolean deleteFilesOnSd() {
		return sendCommand(Operations.CAMERA_DA);
	}

	public boolean deleteLastFileOnSd() {
		return sendCommand(Operations.CAMERA_DL);
	}

	public int fromBoolean(boolean paramBoolean) {
		if (paramBoolean)
			return 1;
		return 0;
	}

	public String getBacPacPassword() {
		if (activeGoProAPI.goProAPIversion != GoProAPIversion.HERO3) {
			System.out.println("ERROR: getBacPacPassword called from non-HERO3");
			return null;
		}
		try {
			GoProProtocolParser localGoProProtocolParser = new GoProProtocolParser(sendGET(Operations.BACPAC_SD));
			byte[] arrayOfByte = new byte[1];
			arrayOfByte[0] = localGoProProtocolParser.extractByte();
			boolean bool = passFail(arrayOfByte);
			Object localObject = null;
			if (bool) {
				String str = localGoProProtocolParser.extractString();
				localObject = str;
			}
			return (String) localObject;
		} catch (Exception localException) {
		}
		return null;
	}

	public BackPack getBackPackInfo() throws Exception {
		// Seems not to be used anywhere
		BackPack localBackPack = new BackPack();
		switch (activeGoProAPI.goProAPIversion) {
			case HERO3:
				GoProProtocolParser localGoProProtocolParser;
				try {
					byte[] arrayOfByte = sendGET(Operations.BACPAC_CV);
					localGoProProtocolParser = new GoProProtocolParser(arrayOfByte);
					if (localGoProProtocolParser.extractResultCode() != GoProProtocolParser.RESULT_IS_OK) {
						return null;
					}
				} catch (Exception localException) {
					throw new Exception("Fail to get backpack info", localException);
				}
				localBackPack.setVersion(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setModel(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setId(localGoProProtocolParser.extractFixedLengthString(2));
				localBackPack.setBootLoaderMajor(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setBootLoaderMinor(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setBootLoaderBuild(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setRevision(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setMajorversion(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setMinorversion(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setBuildversion(localGoProProtocolParser.extractUnsignedByte());
				localBackPack.setWifimac(localGoProProtocolParser.extractFixedLengthString(6));
				localBackPack.setSSID(localGoProProtocolParser.extractString());
				break;
			case HERO4Session:
			case HERO56:
				break;
			case UNDETERMINED:
				break;  // TODO: throw exception.
		}
		return localBackPack;
	}

	public BacPacStatus getBacpacStatus() throws Exception {
		switch (activeGoProAPI.goProAPIversion) {
			case HERO3:
				try {
					byte[] arrayOfByte = sendGET(Operations.BACPAC_SE);
					return getBacpacStatusHERO3(new GoProProtocolParser(arrayOfByte));
				} catch (Exception localException) {
					throw localException;
				}
			case HERO4Session:
			case HERO56:
//				CamFields camfields = new CamFields();
				String rawReply = sendGETStr(Operations.CAMERA_SE);
		        JSONObject json = new JSONObject(rawReply);
		        return getBacpacStatusHERO456(json);
			default:
				return null;
		}
	}

	public BacPacStatus getBacpacStatusHERO3(GoProProtocolParser localGoProProtocolParser) {
		BacPacStatus localBacPacStatus = new BacPacStatus();
		localBacPacStatus.setBacPacBattery(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setWifiMode(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setBlueToothMode(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setRSSI(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setShutterStatus(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setAutoPowerOff(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setBlueToothAudioChannel(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setFileServer(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraPower(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraI2CError(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraReady(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraModel(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraProtocolVersion(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setCameraAttached(localGoProProtocolParser.extractUnsignedByte());
		localBacPacStatus.setBOSSReady(localGoProProtocolParser.extractUnsignedByte());
		return localBacPacStatus;
	}

	public BacPacStatus getBacpacStatusHERO456(JSONObject json) throws Exception {
		BacPacStatus localBacPacStatus = new BacPacStatus();
		// Ref to map between json value and parameters: https://github.com/KonradIT/goprowifihack/blob/master/HERO5/gpControl-HERO5Black.json
		try {
			//JSONObject settings = (JSONObject) json.get("settings");   	//System.out.println("JSON:" + settings.toString(2));
			JSONObject status = (JSONObject) json.get("status");		//System.out.println("JSON:" + status.toString(2));
			localBacPacStatus.setShutterStatus(status.getInt("8"));
			localBacPacStatus.setBOSSReady(1);  // if we reach this point, the camera is ready. There is no better indication.
			localBacPacStatus.setCameraReady(1);
		} catch  (Exception localException) {
			throw new Exception("JSON Failure", localException);
		}
		return localBacPacStatus;
	}

	public int getCameraHLSSegment() {  // Never used
		try {
			byte[] arrayOfByte = sendGET(Operations.CAMERA_HS2);
			//byte[] arrayOfByte = sendGET_OBSOLETE(this.mCameraAddress + Operations.CAMERA_HS2 + "?t=" + this.getToken());
			return new GoProProtocolParser(arrayOfByte).extractUnsignedByte();
		} catch (Exception localException) {
		}
		return -1;
	}

	public CamFields getCameraInfo() {  // Never used
		CamFields localCamFields = new CamFields();
		GoProProtocolParser localGoProProtocolParser;
		try {
			byte[] arrayOfByte = sendGET(Operations.CAMERA_CV);
			//byte[] arrayOfByte = sendGET_OBSOLETE(this.mCameraAddress + Operations.CAMERA_CV + "?t=" + this.getToken());
			localGoProProtocolParser = new GoProProtocolParser(arrayOfByte);
			if (localGoProProtocolParser.extractResultCode() != GoProProtocolParser.RESULT_IS_OK)
				return null;
		} catch (Exception localException) {
			return null;
		}
		localCamFields.setProtocol(localGoProProtocolParser.extractUnsignedByte());
		localCamFields.setModel(localGoProProtocolParser.extractUnsignedByte());
		localCamFields.setVersion(localGoProProtocolParser.extractString());
		localCamFields.setCamname(localGoProProtocolParser.extractString());
		return localCamFields;
	}

	public String getCameraNameCN() {  // Never used
		String str =  activeGoProAPI.ipAddress;
		byte[] arrayOfByte;
		try {
			arrayOfByte = sendGET(Operations.CAMERA_CN);
			//arrayOfByte = sendGET_OBSOLETE(this.mCameraAddress + Operations.CAMERA_CN + "?t=" + this.getToken());
			if ((arrayOfByte == null) || (arrayOfByte.length == 0)
					|| (arrayOfByte[0] == 1))
				return str;
		} catch (Exception localException) {
			return str;
		}
		int i = arrayOfByte[1];
		int j = 0;
		for (int k = 2;; k++) {
			if (j >= i)
				return str;
			if (k < arrayOfByte.length)
				str = str + (char) arrayOfByte[k];
			j++;
		}
	}

	public String FormatJson(String s) {   
	// NOT USED
		s = s.replaceAll("\\{\"", "{\n\""); 		//System.out.println("2:" + s);
		s = s.replaceAll(",", ",\n\t\t"); 		    //System.out.println("3:" + s);
		s = s.replaceAll("\\}\\}", "}\n}\n"); 		//System.out.println("4:" + s);
		s = s.replaceAll("\\n[\t]\\n", "\n"); 		//System.out.println("5:" + s);
		s = s.replaceAll("\\n\"", "\n\t\""); 		//System.out.println("6:" + s);
		s = s.replaceAll("\t\"1\"", "\t\t\"1\""); 	//System.out.println("6:" + s);
		s = s.replaceAll("\t\t\"status", "\t\"status"); 	//System.out.println("6:" + s);
		s = s.replaceAll("\\n\t\"settings", "\t\"settings"); 	//System.out.println("6:" + s);
		return s;
	}
	
	public CamFields getCameraSettings() throws Exception {
		try {
			switch (activeGoProAPI.goProAPIversion) {
			case HERO3:
				byte[] arrayOfByte = sendGET(Operations.CAMERA_SE);
				//byte[] arrayOfByte = sendGET_OBSOLETE(this.mCameraAddress + "/camera/se" + "?t=" + this.getToken());
				return getCameraSettingsHERO3(new GoProProtocolParser(arrayOfByte));
			case HERO4Session:
			case HERO56:
				//CamFields camfields = new CamFields();
				String rawReply = sendGETStr(Operations.CAMERA_SE);
	            JSONObject json = new JSONObject(rawReply);
	            return getCameraSettingsHERO456(json);
			default:
				return null;
			}
		} catch (Exception localException) {
			throw new Exception("Fail to get camera settings", localException);
		}
	}

	public CamFields getCameraSettingsHERO3(GoProProtocolParser paramGoProProtocolParser) {
		CamFields localCamFields = new CamFields();
		if (paramGoProProtocolParser.extractResultCode() != GoProProtocolParser.RESULT_IS_OK)
			return null;
		localCamFields.setMode(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setMicrophoneMode(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setOndefault(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setExposure(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setTimeLapse(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setAutopower(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setFieldOfView(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setPhotoResolution(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setVidres(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setAudioinput(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setPlaymode(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setPlaybackPos(paramGoProProtocolParser.extractInteger());
		localCamFields.setBeepSound(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setLedblink(paramGoProProtocolParser.extractUnsignedByte());
		//int i = paramGoProProtocolParser.extractByte();
		localCamFields.setPreviewActive(true);
		localCamFields.setBattery(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setUsbMode(paramGoProProtocolParser.extractUnsignedByte());
		localCamFields.setPhotosAvailable(paramGoProProtocolParser.extractShort());
		localCamFields.setPhotosOncard(paramGoProProtocolParser.extractShort());
		localCamFields.setVideoAvailable(paramGoProProtocolParser.extractShort());
		localCamFields.setVideoOncard(paramGoProProtocolParser.extractShort());
		localCamFields.setShutter(paramGoProProtocolParser.extractUnsignedByte());
		return localCamFields;
	}

	public CamFields getCameraSettingsHERO456(JSONObject json) throws Exception {
		CamFields localCamFields = new CamFields();
		// Ref to map between json value and parameters: https://github.com/KonradIT/goprowifihack/blob/master/HERO5/gpControl-HERO5Black.json
		try {
			JSONObject status = (JSONObject) json.get("status");		//System.out.println("JSON:" + status.toString(2));
			localCamFields.setMode(status.getInt("43"));
			localCamFields.setShutter(status.getInt("8"));
		} catch  (Exception localException) {
			throw new Exception("JSON Failure", localException);
		}
		return localCamFields;
	}

	public HttpClient newInstance() {
		BasicHttpParams localBasicHttpParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(localBasicHttpParams,
				HttpVersion.HTTP_1_1);
		HttpProtocolParams
				.setContentCharset(localBasicHttpParams, "ISO-8859-1");
		HttpProtocolParams.setUseExpectContinue(localBasicHttpParams, true);
		HttpConnectionParams.setStaleCheckingEnabled(localBasicHttpParams,
				false);
		HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 10000);
		HttpConnectionParams.setSoTimeout(localBasicHttpParams, 10000);
		HttpConnectionParams.setSocketBufferSize(localBasicHttpParams, 8192);
		SchemeRegistry localSchemeRegistry = new SchemeRegistry();
		localSchemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		ConnManagerParams.setMaxTotalConnections(localBasicHttpParams, 1);
		return new DefaultHttpClient(new ThreadSafeClientConnManager(
				localBasicHttpParams, localSchemeRegistry),
				localBasicHttpParams);
	}
	
	private boolean sendCommand(Operations op) {
		try {
			sendGET(op.toString2(getAPIversion()), "");
			return true;
		} catch (Exception localException) {
		}
		return false;
	}

	public boolean sendCommand(Operations op, int paramInt)
			throws Exception {
		String cmdStr = op.toString2(getAPIversion());
		if (cmdStr.startsWith("/gp/") ) {
			return sendCommand(op, Integer.toString(paramInt));
		} else { 
			Object[] arrayOfObject = new Object[1];
			arrayOfObject[0] = Integer.valueOf(paramInt);
			return sendCommand(op, "%"+String.format("%02x", arrayOfObject));
		}
		
	}

	public boolean sendCommand(Operations paramString1, String paramString2)
			throws Exception {
		sendGET(paramString1.toString2(getAPIversion()), paramString2);
		return true;
	}
	
	public byte[] sendGET(Operations op) throws Exception {
		return sendGET(op.toString2(getAPIversion()),"");
	}
	
	public byte[] sendGET(String cmdStr, String paramStr) throws Exception {
		HttpResponse localHttpResponse = SendAndGetHttp(cmdStr, paramStr);
		InputStream localInputStream = localHttpResponse.getEntity().getContent();
		ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
		int j = (int) localHttpResponse.getEntity().getContentLength();
		if (j <= 0)
			j = 128;
		byte[] arrayOfByte = new byte[j];
		while (true) {
			if (localInputStream.read(arrayOfByte, 0, arrayOfByte.length) == -1) {
				localByteArrayOutputStream.flush();
				return localByteArrayOutputStream.toByteArray();
			}
			localByteArrayOutputStream.write(arrayOfByte, 0, arrayOfByte.length);
		}
	}

	public String sendGETStr(Operations op) throws Exception {
		return sendGETStr(op.toString2(getAPIversion()),"");
	}

	public String sendGETStr(String cmdStr, String paramStr) throws Exception {
		HttpResponse localHttpResponse = SendAndGetHttp(cmdStr, paramStr);
		InputStream localInputStream = localHttpResponse.getEntity().getContent();
		if (activeGoProAPI.goProAPIversion != GoProAPIversion.HERO3 ) {
	 		StringBuilder sb = new StringBuilder();
			BufferedReader rd = new BufferedReader(new InputStreamReader(localInputStream, "UTF-8"));

			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + "\n");
			}
			//System.out.println("sendGET Response: " + sb);
			return sb.toString();
		} else {
			System.out.println("ERROR: sendGETHero456 called from non-HERO3");
			return null;
		}
	}

	private HttpResponse SendAndGetHttp(String cmdStr, String paramStr) throws Exception {
		HttpResponse localHttpResponse;
		try {

			System.setProperty("http.keepAlive", "true");
			String s = BuildHttpRequest(cmdStr, paramStr);
			HttpGet localHttpGet = new HttpGet(s);
			RequestConfig requestConfig = RequestConfig.custom()
				       .setSocketTimeout(2000)
				       .setConnectTimeout(2000)
				       .setConnectionRequestTimeout(2000)
				       .build();
			localHttpGet.setConfig(requestConfig);
			HttpClient hc = HttpClients.createDefault();
			localHttpResponse = hc.execute(localHttpGet);
			
			int statusCode = localHttpResponse.getStatusLine().getStatusCode();
			if (statusCode >= 400) {
				localHttpGet.abort();
				System.out.println("SendAndGetHttp (1) exception. Tried to send:" + s);
				System.out.println("SendAndGetHttp (1)                    Reply:" + statusCode);
				throw new IOException("Fail to send GET on " + s + "  ==> HTTP error code = ["
						+ statusCode + "]");
			}
		} catch (Exception localException) {
			System.out.println("SendAndGetHttp (2) exception");
			throw localException;
		}
		return localHttpResponse;
	}

	private String BuildHttpRequest(String cmdStr, String paramStr) {
		String res = "INVALID";
		{
			switch (getAPIversion()) {
				case HERO3:
					res = activeGoProAPI.httpAddr1 + cmdStr + activeGoProAPI.httpAddr2;
					if ( paramStr.length() > 0 ) {
						res = res + "&p=" + paramStr;
					}
					break;
				case HERO4Session:
				case HERO56:
					res = activeGoProAPI.httpAddr1 + cmdStr + activeGoProAPI.httpAddr2;
					if ( paramStr.length() > 0 ) { // any parameters to pass on at all
						// is the command a /gp/..../ type where the parameter should just be appended raw? (like http://10.5.5.9/gp/gpControl/10/1
						if (cmdStr.startsWith("/gp/") && cmdStr.endsWith("/")) {
							res = res + paramStr.substring(paramStr.length() - 1);
						} else { 
							if (res.contains("?")) {   // are there already parameters in the URL?
								res = res + "&";
							} else {
								res = res + "?";
							}
							if (paramStr.contains("=")) {  // check if parameter string contains parameter name. If not, assume "p" to be parameter name
								res = res + paramStr;
							} else {
								res = res + "p=" + paramStr;
							}
						}
					}
					break;
				default:
					break;
			}
		}
		
		System.out.println(timestampStr() + " BuildHttpRequest: " + res);
		return res;
	}
	
	private String timestampStr() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		return dateFormat.format(new Date());
		}
	
    public void WakeOnLan() {
        // from http://www.jibble.org/wake-on-lan/WakeOnLan.java
        
    	String ipStr = activeGoProAPI.ipAddress;
        String macStr = activeGoProAPI.knownMACaddresses;  // TODO if more than one MAC address, this won't work. We will need to split the string. But only used for HERO4SESSION
        int port = activeGoProAPI.port;
        
        try {
            byte[] macBytes = getMacBytes(macStr);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }
            
            InetAddress address = InetAddress.getByName(ipStr);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            Thread.sleep(3000);
            System.out.println("Wake-on-LAN packet sent.");
        }
        catch (Exception e) {
            System.out.println("Failed to send Wake-on-LAN packet: + e");
            System.exit(1);
        }
        
    }
    
    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }
	
	public boolean setBacPacWifiMode(int paramInt) throws Exception {
		return sendCommand(Operations.BACPAC_WI, paramInt);
	}

	public boolean setBackPackPowerCamera(boolean paramBoolean)
			throws Exception {
		return sendCommand(Operations.BACPAC_PW, fromBoolean(paramBoolean));
	}

	public boolean setCamAutoPowerOff(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_AO, paramInt);
	}

	public boolean setCamDateTime(String paramString) {
		try {
			boolean bool = passFail(sendGET(Operations.CAMERA_TM.toString2(getAPIversion()), paramString));
			//boolean bool = passFail(sendGET_OBSOLETE(this.mCameraAddress + "/camera/TM?t=" + this.getToken() + "&p=" + paramString));
			return bool;
		} catch (Exception localException) {
		}
		return false;
	}

	public boolean setCamDefaultMode(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_DM, paramInt);
	}

	public boolean setCamExposure(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_EX, paramInt);
	}

	public boolean setCamFov(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_FV, paramInt);
	}

	public boolean setCamLEDBlink(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_LB, paramInt);
	}

	public boolean setCamLivePreview(boolean paramBoolean) throws Exception {
		if (paramBoolean) {
			return sendCommand(Operations.CAMERA_PV, 0);
		} else {
			return sendCommand(Operations.CAMERA_PV, 2);
		}
	}

	public boolean setCamLocate(boolean paramBoolean) throws Exception {
		return sendCommand(Operations.CAMERA_LL, fromBoolean(paramBoolean));
	}

	public boolean setCamMode(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_CM, paramInt);
	}

	public boolean setCamNtscPal(boolean paramBoolean) throws Exception {
		if (paramBoolean) {
			return sendCommand(Operations.CAMERA_VM, 0);
		} else {
			return sendCommand(Operations.CAMERA_VM, 1);
		}
	}

	public boolean setCamOnScreenDisplay(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_DS, paramInt);
	}

	public boolean setCamPhotoResolution(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_PR, paramInt);
	}

	public boolean setCamProtune(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_PT, paramInt);
	}

	public boolean setCamShutter(boolean paramBoolean) throws Exception {
		return sendCommand(Operations.BACPAC_SH, fromBoolean(paramBoolean));
	}

	public boolean setCamSound(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_BS, paramInt);
	}

	public boolean setCamTimeLapseTI(String paramString) throws Exception {
		return sendCommand(Operations.CAMERA_TI, paramString);
	}

	public boolean setCamUpDown(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_UP, paramInt);
	}

	public boolean setCamVideoResolution(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_VR, paramInt);
	}

	public boolean setCameraHLSSegment(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_HS, paramInt);
	}
	
	public boolean setVideoResolution(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_VV, paramInt);
	}
	
	public boolean setFrameRate(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_FS, paramInt);
	}
	
	public boolean setFov(int paramInt) throws Exception {
		return sendCommand(Operations.CAMERA_FV, paramInt);
	}

	public boolean toBoolean(int paramInt) {
		return paramInt != 0;
	}

	public boolean startRecord() throws Exception {
		return sendCommand(Operations.BACPAC_SH, 1);
	}

	public boolean stopRecord() throws Exception {
		return sendCommand(Operations.BACPAC_SH, 0);
	}

	public boolean turnOnCamera() throws Exception {
		boolean res = true;
		if (!activeGoProAPI.goproHasBeenConnectedOnce)  {
			try {
				// try HERO5
				System.out.println("Try connecting to HERO5/6");
				activeGoProAPI = new GoProAPIconstants(GoProAPIversion.HERO56);
				String cameraName = sendGETStr(Operations.BACPAC_CV);
				int gpStart = cameraName.indexOf("GP");
				cameraName = cameraName.substring(gpStart, gpStart+10);
		    	CamFields gpstat = this.getCameraSettings();
		    	gpstat.setCamname(cameraName);
				System.out.println("Found HERO5/6 " + cameraName);
				res = sendCommand(Operations.CONTROL_PAIR, "deviceName=ViNO");
				return res;
			} catch (Exception e) {
				res = false;
			}
	
			try {
				// try HERO3
				System.out.println("Try connecting to HERO3");
				activeGoProAPI = new GoProAPIconstants(GoProAPIversion.HERO3);
				res = sendCommand(Operations.BACPAC_PW, "%01");
				return res;
			} catch (Exception e) {
				res = false;
			}
	
			try {
			// try HERO4
				System.out.println("Try connecting to HERO4Session");
				activeGoProAPI = new GoProAPIconstants(GoProAPIversion.HERO4Session);
				WakeOnLan();
				res = sendCommand(Operations.BACPAC_CV);
				return res;
			} catch (Exception e) {
				res = false;
			}
	
			if (res == false) {
				throw new Exception("Cannot find any camera online. Connect Wifi and click wifi icon.");
			}
		}
		return res; 
	}
	
	public void SendRawHttpToGoPro5(String completeURL) throws Exception {
		StringBuffer result = new StringBuffer();
			
		String url = String.format(completeURL);  
		
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
	 
		HttpResponse response = client.execute(request);
	 
		int statusCode = response.getStatusLine().getStatusCode();
		System.out.println("SendRawHttpToGoPro5 Response Code: " + statusCode);
		if (statusCode < 400) {
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		 				
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			System.out.println("SendRawHttpToGoPro5 Response: " + result);
		}
		return;
	}
	
	public boolean turnOffCamera() throws Exception {
		return sendCommand(Operations.BACPAC_OFF, 0);
	}

	public boolean changeModeCamera() throws Exception {
		return sendCommand(Operations.CAMERA_CM, 2);
		//return sendCommand(Operations.BACPAC_PW, 2);
	}

	public boolean modeCamera() throws Exception {
		return sendCommand(Operations.CAMERA_CM, 0);
	}

	public boolean modePhoto() throws Exception {
		return sendCommand(Operations.CAMERA_CM, 1);
	}

	public boolean modeBurst() throws Exception {
		return sendCommand(Operations.CAMERA_CM, 2);
	}
	
	public List<String> getFiles() throws Exception {  // NOTE: For HERO5, this function ONLY returns the last file (for speed)
		StringBuffer result = new StringBuffer();
		
		if (activeGoProAPI.goProAPIversion == GoProAPIversion.HERO3 ) {
			for (int i = 0; i < 99; i++) {
				String url = String.format("http://10.5.5.9:8080/videos/DCIM/1%d%dGOPRO/",i);  // TODO 1: make this a little nicer
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet request = new HttpGet(url);
			 
				HttpResponse response = client.execute(request);
			 
				int statusCode = response.getStatusLine().getStatusCode();
				System.out.println("getFiles() Response Code: " + statusCode);
				if (statusCode < 400) {
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				 				
					String line = "";
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}
					break;
				}
			}
		} 
		return createFilesList(result.toString());  
	}
	
	public List<String> createFilesList(String htmlFile) throws Exception {
		
		Document doc = Jsoup.parse(htmlFile); 
		
		Elements links = doc.getElementsByClass("link");
		List<String> files = new ArrayList<String>();
		
		for (Element element : links) {
			if (element.tagName() == "a") {						
				String fileName = element.text();
				String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
				if (tokens[1].equals("MP4")) {
					files.add(fileName);
				}				
			}
		}
				
		return files;
	}

	public String getMostRecentFile(List<String> files) throws Exception {
		
		if (activeGoProAPI.goProAPIversion == GoProAPIversion.HERO3 ) {
			List<Long> fileNumbers = new ArrayList<Long>();
			
			for (String file: files) {
				String[] tokens = file.split("\\.(?=[^\\.]+$)");
				String fileNumberStr = tokens[0].substring(4, 8).replaceFirst("^0+(?!$)", "");
				long fileNumber = Long.parseLong(fileNumberStr);
				fileNumbers.add(fileNumber);
			}
			
			if (!fileNumbers.isEmpty()) {
				long maxInteger = Collections.max(fileNumbers);
				DecimalFormat formatter = new DecimalFormat("0000");
				String mostRecentFile = "GOPR" + formatter.format(maxInteger) + ".MP4"; 
				return mostRecentFile ;
			}
		} else {
	    	CamFields gpstat = this.getCameraSettings();
	    	// We cannot ask HERO5 about files when it's recording 
	    	if (gpstat.getShutter() == 1) {
				return gpstat.getLastFileName();
			} else {
				String rawReply = sendGETStr(Operations.MEDIA_GETALL);
		        JSONObject json = new JSONObject(rawReply);
		        JSONArray media = (JSONArray) json.get("media");		
		        System.out.println("JSON Media:" + media.toString(2));
		        JSONObject tmp;
		        try {
		        	tmp =  (JSONObject) media.get(0);
		        } catch (Exception e) {
		        	return "GOPR0000.MP4";  // FIXME: This won't work. The GoPro does not start from zero if there are no files on SD card. It resumes where it left off before formatting.
		        }
		        System.out.println("JSON tmp:" + tmp.toString(2));
				JSONArray fs = (JSONArray) tmp.optJSONArray("fs");
				System.out.println("JSON fs:" + fs.toString(2));
				System.out.println("JSON fs:" + fs.toString(2));
				String filename = fs.getJSONObject(fs.length()-1).getString("n"); 
		    	gpstat.setLastFileName(filename);
		    	return filename;
			}
		} 

		return "";
	}


}

