package com.psldebugger.iso;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.Test;

import com.psldebugger.jmeter.IsoTemplate;

public class IsoTemplateTest {

	@Test
	public void testRandomLength() {
		
		IsoTemplate isoTemplate = new IsoTemplate();
		String randomString = isoTemplate.Random(12);
		assertEquals(randomString.length(), 12);
	}

	@Test
	public void testCurrentDateLength() {
		
		IsoTemplate isoTemplate = new IsoTemplate();
		String randomString = isoTemplate.CurrentDate("MMDD");
		assertEquals(randomString.length(), 4);
	}
	
	@Test(expected = RuntimeException.class)
	public void testIncorrectDateFormat() {
		
		IsoTemplate isoTemplate = new IsoTemplate();
		isoTemplate.CurrentDate("YYYY");
	}	

	@Test
	public void testCurrentTimeLength() {
		
		IsoTemplate isoTemplate = new IsoTemplate();
		String randomString = isoTemplate.CurrentTime("2460SS");
		assertEquals(randomString.length(), 6);
	}

	@Test(expected = RuntimeException.class)
	public void testIncorrectTimeFormat() {
		
		IsoTemplate isoTemplate = new IsoTemplate();
		isoTemplate.CurrentTime("2460");
	}
	
	@Test
	public void valueParserTest() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		
		Field field = IsoTemplate.class.getDeclaredField("valuePattern");
		field.setAccessible(true);
		Pattern valuePattern = (Pattern)field.get(null);
		
		String line = "#VALUE <CANL1> = <[TERM_OWNER][TERM_CITY][TERM_STATE][TERM_COUNTRY][TERM_ADDRESS][TERM_BRANCH][TERM_REGION][TERM_CLASS]>";
		Matcher valueMatcher = valuePattern.matcher(line);
		assertTrue(valueMatcher.matches());		        
	}	
	
	@Test
	public void messageContentParserTest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		Field field = IsoTemplate.class.getDeclaredField("messageContentPattern");
		field.setAccessible(true);
		Pattern messageContentPattern = (Pattern)field.get(null);
		
		String line = "	<F018> = <[MCC]>   ";
		Matcher matcher = messageContentPattern.matcher(line);
		assertTrue(matcher.matches());		        
	}	

	@Test
	public void loadResourcesTest1() throws URISyntaxException, IOException {
		
		IsoTemplate newTemplate = new IsoTemplate();
		newTemplate.loadResources("ISO_8583.INC", true);
	}
	
	@Test
	public void loadResourcesTest2() throws URISyntaxException, IOException {
		
		IsoTemplate newTemplate = new IsoTemplate();
		newTemplate.loadResources("ISO_8583.INC", false);
		newTemplate.loadResources("DEVICE_ATM_VB24.INC", false);
	}	

	@Test
	public void loadResourceTest3() throws URISyntaxException, IOException {
		
		IsoTemplate newTemplate = new IsoTemplate();
		newTemplate.loadResources("INT_5_P2P_FSS.REQ", true);
	}
	
	@Test
	public void loadResourceTest4() throws URISyntaxException, IOException, ISOException {
		
		IsoTemplate newTemplate = new IsoTemplate();
		newTemplate.loadResources("INT_5_P2P_FSS.REQ", true);
		ISOMsg msg = newTemplate.createMessage();
		
		DateFormat shortDateFormat = new SimpleDateFormat("MMdd");
		String shortDate = shortDateFormat.format(new Date());
		
		//<MID> = <[TRANSACTION_TYPE.AUTHORIZATION_REQUEST]>
		assertEquals(msg.getMTI(), "0100");
		//<F002> = <[CARD_NUMBER]>
		//<F003> = <[PROCESSING_CODE]>
		assertEquals(msg.getString(3), "1330000");
		//<F004> = <[TRANSACTION_AMOUNT]>
		//<F007> = <[%CurrentDate(MMDD)][%CurrentTime(2460SS)]>
		assertTrue(msg.getString(7).startsWith(shortDate));
		//<F011> = <[%Random(6)]>				; STAN
		assertNotNull(msg.getString(11));
		//<F012> = <[%CurrentTime(2460SS)]>	; Terminal Time
		//<F013> = <[%CurrentDate(MMDD)]>		; Terminal Date
		assertEquals(msg.getString(13), shortDate);
		//<F018> = <[MCC]>
		assertEquals(msg.getString(18), "6011");
		//<F022> = <[POS_ENTRY_MODE]>
		assertEquals(msg.getString(22), "001");
		//<F032> = <12347>	; Acquirer ID
		assertEquals(msg.getString(32), "12347");
		//<F037> = <[%Random(12)]>
		assertNotNull(msg.getString(37));
		//<F041> = <T0123456789>	; Terminal
		assertEquals(msg.getString(41), "T0123456789");
		//<F043> = <[TERMINAL_NAME_LOCATION]>
		assertNotNull(msg.getString(43));
		//assertTrue(msg.getString("43.7").startsWith("P-43.7"));
		//<F044> = <[PINCVV_VERIFICATION_RESULT.OK][PINCVV_VERIFICATION_RESULT.OK]>
		assertEquals(msg.getString(44), "11");
		//<F049> = <[TRANSACTION_CURRENCY]>
		assertEquals(msg.getString(49), "643");
		//<F106> = <[TO_ACCOUNT_CURRENCY][ORIGINAL_CURRENCY][TO_ACCOUNT_AMOUNT][ORIGINAL_AMOUNT][SOURCE_ACCOUNT_RATE][DESTINATION_ACCOUNT_RATE]>
		assertTrue(msg.getString(106).startsWith("643643"));
		//<F108> = <[%Random(12)]>
		assertNotNull(msg.getString(108));
		//<F121> = <[TRANCATEGORY][DRAFTCAPTURE][CVV2][CLERK][INVOICENUM][SEQNUM]>
		assertTrue(msg.getString("121").startsWith("005"));
		//<F123> = <P2=4714870080883056>
		assertTrue(msg.getString("123").startsWith("P2="));
		//<F124> = <IPS=NSPK48_20=G0105NSPK48_30=31121972NSPK22=820NSPK61=000010090NSPK63=00015092452976337980MPDNSPK48_14=G2C>
		assertTrue(msg.getString("124").startsWith("IPS="));		
	}
	
	@Test
	public void loadResourceTest5() throws URISyntaxException, IOException, ISOException {
		
		IsoTemplate newTemplate = new IsoTemplate();
		newTemplate.loadResources("INT_5_BALANCE.REQ", true);
		ISOMsg msg = newTemplate.createMessage();
		
		assertEquals(msg.getString(18), "6011");		
	}
}
