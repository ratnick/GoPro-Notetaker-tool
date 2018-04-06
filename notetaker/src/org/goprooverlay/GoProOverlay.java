package org.goprooverlay;


import java.util.List;


//import org.gopro.core.GoProHelper;
import org.gopro.core.model.BacPacStatus;
import org.gopro.core.model.ENCameraBoss;
//import org.gopro.core.model.ENCameraPowerStatus;
//import org.gopro.core.model.ENCameraReady;

import org.gopro.core.model.CamFields;
import org.gopro.main.GoProApi;

public class GoProOverlay {


	public void startRecordCheck(GoProApi gopro) throws Exception {
		if ( isGoProReadyToReceiveCmd(gopro) ) {
			gopro.getHelper().startRecord();
		}
	}

	public void stopRecordCheck(GoProApi gopro) throws Exception {
		if ( isGoProReadyToReceiveCmd(gopro) ) {
			gopro.getHelper().stopRecord();
		}
	}

		public void setBeep(int vol, GoProApi gopro) throws Exception {
			// vol 00,01,02 (0,70%,100%)
			gopro.getHelper().setCamSound(vol);
		}
		
		public List<String> getFiles(GoProApi gopro) throws Exception {
			return gopro.getHelper().getFiles();
		}

		public boolean isGoProRecording(GoProApi gopro)  {
		
			CamFields gpstat;
			try {
				gpstat = gopro.getHelper().getCameraSettings();
			
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
		
		public CamFields verifyIfGoProIsRecordingNEW(GoProApi gopro) throws Exception {
		
			CamFields gpstat = gopro.getHelper().getCameraSettings();
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
		
		public BacPacStatus verifyIfGoProIsRecording(GoProApi gopro) throws Exception {
		
			BacPacStatus bacpacStatus = gopro.getHelper().getBacpacStatus();
			int shutterStatus = bacpacStatus.getShutterStatus();
			if (shutterStatus == 123) {
				throw new Exception(
						"The go pro is not ready. Check if it is power on.");
			}
		
			return bacpacStatus;
		}
		
		private boolean isGoProReadyToReceiveCmd(GoProApi gopro) throws Exception {
			BacPacStatus bacpacStatus = gopro.getHelper().getBacpacStatus();
			int bossReady = bacpacStatus.getBOSSReady();

			if (ENCameraBoss.READY_TO_RECEIVE_CMD.getCode() == bossReady) {
				return true;
			} else {
				System.out.println("isGoProReadyToReceiveCmd: Exception");
				throw new Exception("Gopro not ready to receive url cmd (isGoProReadyToReceiveCmd).");
			}
		}
		
		public String getMostRecentFileName(GoProApi gopro) throws Exception {
			System.out.println("Getting most recent file");
			try {
				verifyIfGoProIsRecordingNEW(gopro);  // NNR ID11 test this
				List<String> files = getFiles(gopro);
				for (String f:files) {
					System.out.println("FILE "+f);
				}
				String file = gopro.helper.getMostRecentFile(files);
				return file;
				
			} catch (Exception e) {
				throw new Exception();
			}
		}
		
		public void connectAndInit(GoProApi gopro) throws Exception{
			if (gopro.powerAndWaitUntilIsReady()) {
				stopRecordCheck(gopro);			
				Thread.sleep(2000);
				sendConfiguration(gopro);
			}
		}

		public void deleteAll(GoProApi gopro) throws Exception{
			gopro.helper.deleteFilesOnSd();
		}	

		public void sendConfiguration(GoProApi gopro) throws Exception{
			
			try {
				// {'CM': '00', 'VV': '03', 'FS': '04', 'FV': '00', 'AO': '00', 'PV': '00'}
				// CM = Camera Mode, VV = Resolution, FS = Framerate, FV = FOV, AO = Autopower, PV = Preview
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setCamMode(0);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setVideoResolution(3);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setFrameRate(4);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setFov(0);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setCamAutoPowerOff(0);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setCamLEDBlink(2);
				gopro.waitUntilIsReadyToReceiveCmd();
				gopro.helper.setCamNtscPal(true);
				//this.helper.setCamLivePreview(false);
			} catch (Exception e) {
				throw new Exception();
			}
		}


}
