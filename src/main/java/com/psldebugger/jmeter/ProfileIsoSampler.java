package com.psldebugger.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psldebugger.iso.Gateway;

public class ProfileIsoSampler extends AbstractJavaSamplerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileIsoSampler.class);
	
	private static final String HOST = "host";
	private static final String PORT = "port";
	private static final String TEMPLATE = "template";
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
        defaultParameters.addArgument(TEMPLATE, "INT_5_BALANCE.REQ");
        defaultParameters.addArgument(CARD, "0123456789ABCDEF");

        return defaultParameters;    	
    }
    
	@Override
	public SampleResult runTest(JavaSamplerContext ctx) {

		String host = ctx.getParameter(HOST);
		String port = ctx.getParameter(PORT);
        String template = ctx.getParameter(TEMPLATE);
        String cardNumber = ctx.getParameter(CARD);

        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();
        
		try {

			//Builder builder = new Builder();
			//ISOMsg msg = builder.createBalanceInquery("4093980008805528");
			ISOMsg msg = IsoTemplate.createMessage(template);
			msg.set(2, cardNumber);
			
			LOGGER.info(msg.toString());			
			Gateway gateway = new Gateway();
			ISOMsg response = gateway.send(host, Integer.valueOf(port), msg);			
			LOGGER.info(response.toString());
			
			String responseCodeString = (String)response.getValue(39);
		    int responseCode = Integer.valueOf(responseCodeString);
		    // 00	None
		    // 01 	Approved
		    if (responseCode <= 1) {
		    	
		    	sampleResult.sampleEnd();
				sampleResult.setSuccessful(Boolean.TRUE);
				sampleResult.setResponseCodeOK();
				// Search for the string in NSMLOG
	            sampleResult.setResponseMessage((String)response.getValue(37));
		    	LOGGER.info("Search NSMLOG for: " + (String)response.getValue(37));	            
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
