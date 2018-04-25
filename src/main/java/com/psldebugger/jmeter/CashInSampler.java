package com.psldebugger.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psldebugger.iso.Gateway;

public class CashInSampler extends AbstractJavaSamplerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(CashInSampler.class);
	
	private static final String HOST = "host";
	private static final String PORT = "port";
	private static final String ADVICE_TEMPLATE = "advice template";
	private static final String REQUEST_TEMPLATE = "request template";
	private static final String CARD = "card";
	
	@Override
    public void setupTest(JavaSamplerContext context){

    	super.setupTest(context);
    }
    
    @Override
    public Arguments getDefaultParameters() {  

        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(HOST, "172.29.7.82");
        defaultParameters.addArgument(PORT, "19260");
        defaultParameters.addArgument(ADVICE_TEMPLATE, "INT_5_AUTHORIZATION_ADVICE_020.REQ");
        defaultParameters.addArgument(REQUEST_TEMPLATE, "INT_5_AUTHORIZATION_REQUEST_020.REQ");
        defaultParameters.addArgument(CARD, "0123456789ABCDEF");

        return defaultParameters;    	
    }
    
	@Override
	public SampleResult runTest(JavaSamplerContext ctx) {

		String host = ctx.getParameter(HOST);
		String port = ctx.getParameter(PORT);
        String adviceTemplate = ctx.getParameter(ADVICE_TEMPLATE);
        String requestTemplate = ctx.getParameter(REQUEST_TEMPLATE);
        String cardNumber = ctx.getParameter(CARD);

        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();
        
		try {

			ISOMsg msg = IsoTemplate.createMessage(adviceTemplate);
			msg.set(2, cardNumber);
						
			LOGGER.info(msg.toString());
			Gateway gateway = new Gateway();
			LOGGER.info("Submitting authorization advice");
			ISOMsg response = gateway.send(host, Integer.valueOf(port), msg);			
			LOGGER.info(response.toString());
			
			String adviceCodeString = (String)response.getValue(39);
		    int adviceCode = Integer.valueOf(adviceCodeString);
		    // 00	None
		    // 01 	Approved
		    if (adviceCode <= 1) {
		    	
		    	LOGGER.info("Advice succeeded. Search NSMLOG for: " + (String)response.getValue(37));	            
			}
			else {

				sampleResult.sampleEnd();
		    	sampleResult.setSuccessful(Boolean.FALSE);
				sampleResult.setResponseCodeOK();
	            sampleResult.setResponseMessage(adviceCodeString);
				LOGGER.error("Failed. Response code: " + (String)response.getValue(37));
				return sampleResult;
			}		    
		    
		    ISOMsg msg2 = IsoTemplate.createMessage(requestTemplate);
		    msg2.set(2, cardNumber);
		    // Transaction amount
		    msg2.set(4, msg.getString(4));
		    // EXTRRN (Field 108) must be the same value as returned from the Cash-In Phase 1 log file (Authorization)
		    String f108 = (String)response.getValue(108); 		    
		    msg2.set(108, f108);
		    LOGGER.info("Submitting request");
		    ISOMsg response2 = gateway.send(host, Integer.valueOf(port), msg2);
		    		    
			String responseCodeString = (String)response2.getValue(39);
		    int responseCode = Integer.valueOf(responseCodeString);
		    // 00	None
		    // 01 	Approved
		    if (responseCode <= 1) {
		    	
		    	sampleResult.sampleEnd();
				sampleResult.setSuccessful(Boolean.TRUE);
				sampleResult.setResponseCodeOK();
				// Search for the string in NSMLOG
	            sampleResult.setResponseMessage((String)response.getValue(37));
		    	LOGGER.info("Request succeeded. Search NSMLOG for: " + (String)response.getValue(37));	            
			}
			else {

				sampleResult.sampleEnd();
		    	sampleResult.setSuccessful(Boolean.FALSE);
				sampleResult.setResponseCodeOK();
	            sampleResult.setResponseMessage(responseCodeString);
				LOGGER.error("Failed. Response code: " + (String)response.getValue(37));	            
			}
		}
		catch(Throwable ex) {
			
			LOGGER.error("Error message: ", ex);
			sampleResult.sampleEnd();
            sampleResult.setSuccessful(Boolean.FALSE);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessage(ex.getMessage());
		}
		
		return sampleResult;
	}

}
