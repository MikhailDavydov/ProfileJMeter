package com.psldebugger.iso;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.channel.BASE24TCPChannel;

public class ProfileTCPChannel extends BASE24TCPChannel {

	protected int getHeaderLength() {
		
		return 8;	//return super.getHeaderLength();
	}
  
	protected int getMessageLength() throws IOException, ISOException {
		
		return super.getMessageLength() + 1;
	}

	protected void getMessageTrailler() throws IOException {
	
	}

}
