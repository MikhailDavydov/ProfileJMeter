package com.psldebugger.iso;

import java.io.IOException;
import java.net.URISyntaxException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.Test;

import com.psldebugger.jmeter.IsoTemplate;

public class GatewayTest {
	
	//@Test
	public void fssTest() throws IOException, ISOException, URISyntaxException {

		ISOMsg msg = IsoTemplate.createMessage("INT_5_P2P_FSS.REQ");
		// Card number
		msg.set(2, "4893500000060864");
		// Transaction amount
		msg.set(4, "12300");
		Gateway gateway = new Gateway();
		gateway.send("172.29.7.82", 19260, msg);		
	}

	//@Test
	public void withdrawalTest() throws IOException, ISOException, URISyntaxException {

		ISOMsg msg = IsoTemplate.createMessage("INT_5_ATM_WITHDRAWAL_010.REQ");
		msg.set(2, "4893500000060864");
		Gateway gateway = new Gateway();
		gateway.send("172.29.7.82", 19260, msg);		
	}
	
	//@Test
	public void p2pDebit() throws IOException, ISOException, URISyntaxException {

		ISOMsg msg = IsoTemplate.createMessage("INT_5_ATM_P2P_DEBIT_063.REQ");
		msg.set(2, "4893500000060864");
		Gateway gateway = new Gateway();
		gateway.send("172.29.7.82", 19260, msg);		
	}
	
	//@Test
	public void p2pCredit() throws IOException, ISOException, URISyntaxException {

		ISOMsg msg = IsoTemplate.createMessage("INT_5_ATM_P2P_CREDIT.REQ");
		msg.set(2, "4893500000060864");
		Gateway gateway = new Gateway();
		gateway.send("172.29.7.82", 19260, msg);		
	}	

	//@Test
	public void balance() throws IOException, ISOException, URISyntaxException {

		ISOMsg msg = IsoTemplate.createMessage("INT_5_BALANCE.REQ");
		msg.set(2, "4893500000060864");
		Gateway gateway = new Gateway();
		gateway.send("172.29.7.82", 19260, msg);		
	}	
}
