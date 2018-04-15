package com.psldebugger.iso;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.BASE24TCPChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;
import org.slf4j.LoggerFactory;

import com.psldebugger.jmeter.ProfileXmlSampler;

public class Gateway {
	
	//private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);	

	public ISOMsg send(String host, int port, ISOMsg msg) throws ISOException, IOException {
		
		// Check with
		// select ZPRTCLID,zprtclver from UTBLEXTINT where INTRFACE='ISOATM'
		msg.setHeader("A4M04000".getBytes());
		msg.setDirection(ISOMsg.OUTGOING);
		
		Logger logger = new Logger();
		logger.addListener(new SimpleLogListener(System.out));
		
		GenericPackager packager = new GenericPackager(getClass().getResourceAsStream("/iso/base24.xml"));
	    msg.setPackager(packager);
	      
	    BASE24TCPChannel clientChannel = new ProfileTCPChannel();
	    ((LogSource)clientChannel).setLogger(logger, "channel");
	    clientChannel.setPackager(packager);
	    clientChannel.setHost(host, port);
	    clientChannel.connect();
	    clientChannel.send(msg);
	    return clientChannel.receive();
	}
}
