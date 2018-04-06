package jmtp;

import be.derycke.pieter.com.COMException;

public class PortableDeviceToHostImpl32 {

    public PortableDeviceToHostImpl32() {
    }
    
    public native void copyFromPortableDeviceToHost(String objectId, String destPath, PortableDevice pDevice) throws COMException;

}
