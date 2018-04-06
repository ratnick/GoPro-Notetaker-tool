package org.gopro.core;

import org.gopro.core.GoProHelper;

public enum Operations {

	//CAMERA_CN ( "/camera/CN", "" ),
	BACPAC_SD 	( "/bacpac/sd", "" ),
	BACPAC_WI 	( "/bacpac/WI", "" ),
	BACPAC_SE 	( "/bacpac/se", "" ),
	BACPAC_CV 	( "/bacpac/cv", "" ),
	BACPAC_SH 	( "/bacpac/SH", "/gp/gpControl/command/shutter" ),
	BACPAC_PW 	( "/bacpac/PW", "" ),
	BACPAC_OFF  ( "/bacpac/PW", "/gp/gpControl/command/system/sleep" ),
	CONTROL_PAIR ( "n/a", "/gp/gpControl/command/wireless/pair/complete?success=1" ),
	CAMERA_TI 	( "/camera/TI", ""),
	CAMERA_VR 	( "/camera/VR", "/gp/gpControl/setting/2/"),
	CAMERA_BS 	( "/camera/BS", "/gp/gpControl/setting/87/" ),
	CAMERA_PT 	( "/camera/PT", "" ),
	CAMERA_DS 	( "/camera/DS", "/gp/gpControl/setting/58/" ),
	CAMERA_PR 	( "/camera/PR", "" ),
	CAMERA_VM 	( "/camera/VM", "/gp/gpControl/setting/57/" ),
	CAMERA_LL 	( "/camera/LL", "/gp/gpControl/command/system/locate" ),
	CAMERA_PV 	( "/camera/PV", "" ),
	CAMERA_LB 	( "/camera/LB", "/gp/gpControl/setting/55/" ),
	CAMERA_FV 	( "/camera/FV", "/gp/gpControl/setting/4/" ),
	CAMERA_EX 	( "/camera/EX", "" ),
	
	CAMERA_CN	("/camera/cn", "" ),
	CAMERA_SX 	( "/camera/sx", "" ),
	CAMERA_HS2 	( "/camera/hs", "" ),
	CAMERA_CV 	( "/camera/cv", "" ),
	CAMERA_DL 	( "/camera/DL", "/gp/gpControl/command/storage/delete/last" ),
	CAMERA_DA 	( "/camera/DA", "/gp/gpControl/command/storage/delete/all" ),
	CAMERA_UP 	( "/camera/UP", "" ),
	CAMERA_HS 	( "/camera/HS", "" ),
	CAMERA_DM 	( "/camera/DM", "" ),
	CAMERA_CM 	( "/camera/CM", "/gp/gpControl/command/mode" ),
	CAMERA_AO 	("/camera/AO", "/gp/gpControl/setting/59/" ),

	CAMERA_VV 	("/camera/VV", "/gp/gpControl/setting/2/" ),
	CAMERA_FS 	("/camera/FS", "/gp/gpControl/setting/3/" ),
	CAMERA_SE 	("/camera/se", "/gp/gpControl/status" ),
	CAMERA_TM 	("/camera/TE", "" ),
	MEDIA_GETALL ("", "/gp/gpMediaList");
	 
	private String cmd_hero3;
	private String cmd_hero456;

	private Operations(String cmd3, String cmd456){
		this.setCmdH3(cmd3);
		this.setCmdH456(cmd456);
	}

	public void setCmdH3(String cmd) {
		this.cmd_hero3 = cmd;
	}
	
	public void setCmdH456(String cmd) {
		// blank means same as HERO3
		if (cmd.equals("")) {
			this.cmd_hero456 = this.cmd_hero3;
		} else {
			this.cmd_hero456 = cmd;
		}
	}
	
	public String getCmd(GoProHelper.GoProAPIversion api) {
		String cmd = "N/A";
		
		switch (api) {
			case HERO3:
				cmd = cmd_hero3;
				break;
			case HERO4Session:
			case HERO56:
				cmd = cmd_hero456;
				break;
			default:
				break;
		}
		return cmd;
	}

	public String toString(GoProHelper.GoProAPIversion api){
		return getCmd(api);
	}
	
	public String toString2(GoProHelper.GoProAPIversion api) {
		String res = null;
		res = this.toString(api);
		return res;
	}
}

// status JSON from https://github.com/KonradIT/goprowifihack/blob/master/HERO5/gpControl-HERO5Black.json
