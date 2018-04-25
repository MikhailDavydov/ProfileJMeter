package com.psldebugger.iso;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.BASE24TCPChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

import com.psldebugger.jmeter.Utils;

public class Gateway {
	
	static String getHeader() throws URISyntaxException, IOException {

		String resourceName = "/iso/header.txt";
		URI fileURI = Gateway.class.getResource(resourceName).toURI();
		Utils.initFileSystem(fileURI);
		Path path = Paths.get(fileURI);       
		List<String> lines = Files.readAllLines(path);		
		for (String line: lines) {
			
			if (line.length() > 0 && !line.startsWith("#"))
				return line;
		}
		
		return null;		
	}
	
	String header;
	
	public ISOMsg send(String host, int port, ISOMsg msg) throws ISOException, IOException, URISyntaxException {
		
		if (header == null)
			header = getHeader();
		
		msg.setHeader(header.getBytes());
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
