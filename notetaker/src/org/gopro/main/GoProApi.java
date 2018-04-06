package org.gopro.main;

import java.util.List;

import org.gopro.core.GoProHelper;
import org.gopro.core.GoProHelper.GoProAPIversion;
import org.gopro.core.model.BacPacStatus;
import org.gopro.core.model.CamFields;
import org.gopro.core.model.ENCameraBoss;
import org.gopro.core.model.ENCameraPowerStatus;
import org.gopro.core.model.ENCameraReady;

public class GoProApi {

	public static final int _CONNECT_POLLINGTIME = 4000;
	public static final int _CAMERA_STATUS_POLLINGTIME = 5000;
	public static final int _RETRY_OPERATION = 3;
	public static final int _HERO5_KEEPALIVE_POLLINGTIME = 20000;
	public static final int _STARTUP_PAUSE = 25000;
	public static final int _DELAY_UNTIL_NEXT_CMD = 3500;
	
	public GoProAPIversion getAPIversion() {
		return this.helper.getAPIversion();
	}
	
	public String getActiveRecordingFile() throws Exception {
		String file;
		System.out.println("Getting most recent file");
		
		try {
			List<String> files = null;
			if ( this.helper.getAPIversion() == GoProAPIversion.HERO3 ) {
				verifyIfGoProIsRecordingNEW();  // NNR ID11 test this
				files = this.helper.getFiles();
				for (String f:files) {
					System.out.println("FILE "+f);
				}
			}

			file = this.helper.getMostRecentFile(files);  // files not used for HERO5
			
		} catch (Exception e) {
			throw new Exception();
		}

		if ( this.helper.getAPIversion() != GoProAPIversion.HERO3 ) {
			// guess on next file name, i.e. sequence number + 1
			int fileNbr = Integer.parseInt(file.substring(5, 8));
			fileNbr++;
			file = String.format("GOPR%04d.MP4", fileNbr);
		}
		return file;
	}
	
	public void deleteAll() throws Exception{
		this.helper.deleteFilesOnSd();
	}	
	
	public void connectAndInit() throws Exception{
		if (powerAndWaitUntilIsReady()) {
			stopRecord();			
			Thread.sleep(2000);
			sendConfiguration();
		}
	}

	public void sendConfiguration() throws Exception{
		
		try {
			// {'CM': '00', 'VV': '03', 'FS': '04', 'FV': '00', 'AO': '00', 'PV': '00'}
			// CM = Camera Mode, VV = Resolution, FS = Framerate, FV = FOV, AO = Autopower, PV = Preview
			waitUntilIsReadyToReceiveCmd();
			this.helper.setCamMode(0);
			waitUntilIsReadyToReceiveCmd();
			this.helper.setVideoResolution(3);
			waitUntilIsReadyToReceiveCmd();
			if ( this.helper.getAPIversion() == GoProAPIversion.HERO3 ) {
				this.helper.setFrameRate(4);
			} else {
				this.helper.setFrameRate(8);
			}
			waitUntilIsReadyToReceiveCmd();
			this.helper.setFov(0);
			waitUntilIsReadyToReceiveCmd();
			this.helper.setCamAutoPowerOff(0);
			waitUntilIsReadyToReceiveCmd();
			this.helper.setCamLEDBlink(2);
			waitUntilIsReadyToReceiveCmd();
			this.helper.setCamNtscPal(true);
			//this.helper.setCamLivePreview(false);
		} catch (Exception e) {
			throw new Exception();
		}
	}
	
	public void powerOnAndStartRecord() throws Exception {
		boolean goproIsReady = false;

		try {
			verifyIfGoProIsPowerOn();

			verifyIfGoProIsReadyToRecord();

			goproIsReady = true;
		} catch (Exception e) {
			System.out
					.println("The gopro is not poweron. Let try again power on. ["
							+ e.getMessage() + "]");
			goproIsReady = powerAndWaitUntilIsReady();
		}

		if (goproIsReady) {

			waitUntilIsReadyToReceiveCmd();

			System.out.println("Starting record");
			startRecord();

			System.out.println("Started.");
		}
	}

	public void stopRecordAndPowerOff() throws Exception {

		System.out.println("Stopping record");
		stopRecord();

		System.out.println("Stopped.");

		Thread.sleep(_CONNECT_POLLINGTIME);

		System.out.println("Power Off Go Pro");
		powerOff();

		System.out.println("Power Off.");
	}

	private BacPacStatus verifyIfGoProIsReadyToRecord() throws Exception {

		BacPacStatus bacpacStatus = getHelper().getBacpacStatus();
		int cameraReady = bacpacStatus.getBOSSReady();  // NNR 170114 
		if (ENCameraReady.READY.getCode() != cameraReady) {
			throw new Exception(
					"The go pro is not ready. Check if it is power on.");
		}

		return bacpacStatus;
	}

	public BacPacStatus verifyIfGoProIsPowerOn() throws Exception {

		System.out.println("Verifying if go pro is power on...");

		BacPacStatus bacpacStatus = getHelper().getBacpacStatus();

		int cameraPower = bacpacStatus.getCameraPower();
		System.out.println("Camera power ? " + cameraPower);
		if (ENCameraPowerStatus.POWERON.getCode() != cameraPower) {
			throw new Exception("The go pro is not power on.");
		}
		return bacpacStatus;
	}
	
	public boolean isGoProRecording()  {

    	CamFields gpstat;
		try {
			gpstat = getHelper().getCameraSettings();
		
			/* System.out.println("CAMERA status (2): " );
			System.out.println("   -  Mode="+gpstat.getMode());
		    System.out.println("   -  MicrophoneMode="+gpstat.getMicrophoneMode());
		    System.out.println("   -  Vidres="+gpstat.getVidres());
		    System.out.println("   -  Battery="+gpstat.getBattery());
		    System.out.println("   -  UsbMode="+gpstat.getUsbMode());
		    System.out.println("   -  Shutter="+gpstat.getShutter());  */
	
			return (gpstat.getShutter() == 1);
		} catch (Exception e) {
			//e.printStackTrace();
			return false;			// throw new Exception("GoPro is not recording as expected. Check if power is on");
		}
	}

	public CamFields verifyIfGoProIsRecordingNEW() throws Exception {

    	CamFields gpstat = getHelper().getCameraSettings();
		/*System.out.println("CAMERA status: " );
	    System.out.println("   -  Mode="+gpstat.getMode());
	    System.out.println("   -  MicrophoneMode="+gpstat.getMicrophoneMode());
	    System.out.println("   -  Vidres="+gpstat.getVidres());
	    System.out.println("   -  Battery="+gpstat.getBattery());
	    System.out.println("   -  UsbMode="+gpstat.getUsbMode());
	    System.out.println("   -  Shutter="+gpstat.getShutter());
        */
		if ( gpstat.getShutter() == 1) {
			return gpstat;
		} else {
			throw new Exception("GoPro is not recording as expected. Check is power is on");
		}
		
	}

	public String getCameraName() throws Exception {
    	CamFields gpstat = getHelper().getCameraSettings();
		return gpstat.getCamname();
	}
	
	public boolean checkAndSetSSID(String ssid) {
		if (ssid == null) ssid="";
		
		if ( ssid.startsWith("UXCC") ) {
			this.helper.setAPIversion(GoProAPIversion.HERO3);
			this.helper.setSSID(ssid);
			this.helper.setGoproHasBeenConnectedOnce(true);
			return true;
		} else if ( ssid.startsWith("GP") )  {
			this.helper.setAPIversion(GoProAPIversion.HERO56);
			this.helper.setSSID(ssid);
			this.helper.setGoproHasBeenConnectedOnce(true);
			return true;
		} else {
			return false;
		}
	}
	
	public BacPacStatus verifyIfGoProIsRecording() throws Exception {

		BacPacStatus bacpacStatus = getHelper().getBacpacStatus();
		int shutterStatus = bacpacStatus.getShutterStatus();
		if (shutterStatus == 123) {
			throw new Exception("The go pro is not ready. Check if it is power on.");
		}

		return bacpacStatus;
	}

	public boolean powerAndWaitUntilIsReady() throws Exception {

		boolean result = false;

		powerOn();
		System.out.println("Sending power on to gopro");

		int timeout = 0;

		for (int i = 0; i < _RETRY_OPERATION; i++) {

			try {
				verifyIfGoProIsReadyToRecord();

				result = true;

				break;
			} catch (Exception e) {
				System.out
						.println("Fail to check if gopro is ready. Let try again. Waiting time ["
								+ _CONNECT_POLLINGTIME + "]");
			}

			Thread.sleep(_CONNECT_POLLINGTIME);

			timeout++;
		}
		if (timeout == _RETRY_OPERATION) {
			throw new Exception(
					"The wait has timeout[waitUntilIsBOSSReady], check if the go pro is working correctly.");
		}

		return result;
	}

	public boolean waitUntilIsReadyToReceiveCmd() throws Exception {

		boolean result = false;

		int timeout = 0;
		for (int i = 0; i < _RETRY_OPERATION; i++) {

			try {
				verifyIfGoProIsReadyToReceiveCmd();

				result = true;
				break;
			} catch (Exception e) {
				System.out
						.println("Fail to check if gopro is ready. Let try again. Waiting time ["
								+ _CONNECT_POLLINGTIME + "]");
			}
			Thread.sleep(_CONNECT_POLLINGTIME);

			timeout++;
		}

		if (timeout == _RETRY_OPERATION) {
			throw new Exception(
					"The wait has timeout[waitUntilIsReadyToReceiveCmd], check if the go pro is working correctly.");
		}

		return result;
	}

	private BacPacStatus verifyIfGoProIsReadyToReceiveCmd() throws Exception {

		BacPacStatus bacpacStatus = getHelper().getBacpacStatus();
		int bossReady = bacpacStatus.getBOSSReady();
		if (ENCameraBoss.READY_TO_RECEIVE_CMD.getCode() != bossReady) {
			throw new Exception(
					"The go pro is not ready to receive url cmd(i.e start record).");
		}

		return bacpacStatus;
	}

	private boolean isGoProReadyToReceiveCmd() throws Exception {

		BacPacStatus bacpacStatus = getHelper().getBacpacStatus();
		int bossReady = bacpacStatus.getBOSSReady();
		if (ENCameraBoss.READY_TO_RECEIVE_CMD.getCode() == bossReady) {
			return true;
		} else {
			System.out.println("isGoProReadyToReceiveCmd: Exception");
			throw new Exception("Gopro not ready to receive url cmd (isGoProReadyToReceiveCmd).");
		}
	}
		
	public GoProHelper helper;

	public GoProApi() {
		setHelper(new GoProHelper());   
	}

	public GoProHelper getHelper() {
		return helper;
	}

	public void setHelper(GoProHelper helper) {
		this.helper = helper;
	}

	public void startRecord() throws Exception {
		if ( isGoProReadyToReceiveCmd() ) {
			getHelper().startRecord();
		}
	}

	public void stopRecord() throws Exception {
		if ( isGoProReadyToReceiveCmd() ) {
			getHelper().stopRecord();
		}
	}

	public void takeAndDeletePicture() throws Exception {
		getHelper().startRecord();
		Thread.sleep(_DELAY_UNTIL_NEXT_CMD);
		getHelper().stopRecord();
		Thread.sleep(_DELAY_UNTIL_NEXT_CMD);
		// If only For some super strange reason (that I don't know) the gopro does not store the taken picture. So don't try to delete it because  you will delete the videos instead
		getHelper().deleteLastFileOnSd();  
	}

	public void powerOn() throws Exception {
		getHelper().turnOnCamera();
	}

	public void powerOff() throws Exception {
		getHelper().turnOffCamera();
	}
	
	public void setBeep(int vol) throws Exception {
		// vol 00,01,02 (0,70%,100%)
		getHelper().setCamSound(vol);
	}
	
	public List<String> getFiles() throws Exception {
		return getHelper().getFiles();
	}
}