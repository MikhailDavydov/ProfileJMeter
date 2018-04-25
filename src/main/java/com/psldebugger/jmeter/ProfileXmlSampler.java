package com.psldebugger.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import org.apache.jmeter.protocol.jdbc.config.DataSourceElement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProfileXmlSampler extends AbstractJavaSamplerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileXmlSampler.class);
	
	private static final String DATA_SOURCE = "data source";
	private static final String INTERFACE = "interface";
	private static final String ZCONTRACT = "zcontract";
	
	@Override
    public void setupTest(JavaSamplerContext context){

    	super.setupTest(context);
    }
    
    @Override
    public Arguments getDefaultParameters() {  

        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(DATA_SOURCE, "Profile");
        defaultParameters.addArgument(INTERFACE, "DEP_INFO");
        defaultParameters.addArgument(ZCONTRACT, "");

        return defaultParameters;    	
    }
    
    static ConcurrentMap<String, String> templateMap = new ConcurrentHashMap<String, String>();
    
	@Override
	public SampleResult runTest(JavaSamplerContext ctx) {
		
		String dataSource = ctx.getParameter(DATA_SOURCE);
		String intrface = ctx.getParameter(INTERFACE);
        String zcontract = ctx.getParameter(ZCONTRACT);

        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();
        
		try (Connection conn = DataSourceElement.getConnection(dataSource);) {

			String xmlTemplate = null;
			if (!templateMap.containsKey(intrface)) {
				
				xmlTemplate = readResource("/xml/" + intrface + ".xml");
				templateMap.putIfAbsent(intrface, xmlTemplate);
			}

			xmlTemplate = templateMap.get(intrface);
			
			String xml = xmlTemplate;
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
			java.util.Date today = Calendar.getInstance().getTime();
			String dateString = df.format(today);
			xml = xml.replace("%%timestamp%%", dateString);
			xml = xml.replace("%%messageid%%", "msg_" + this.getClass().getName() + "_" + Thread.currentThread().getName() + "_" + dateString);
			xml = xml.replace("%%zcontract%%", zcontract);	
			
			CallableStatement stmt = conn.prepareCall("{call xmrpc(9000,1,?,?,SPV_AUTH=*)}");
			stmt.setString(1, xml);
			stmt.registerOutParameter(2, java.sql.Types.VARCHAR);
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				
				String response = rs.getString(1);
				
				LOGGER.info("ProfileXmlSampler.zcontract: " +  zcontract);
				LOGGER.info("ProfileXmlSampler.request" + xml);
				LOGGER.info("ProfileXmlSampler.response" + response);		
				
	            sampleResult.sampleEnd();
	            
				if (response.contains("<retCode>FAIL</retCode>")) {
							
					final String beginTag = "<error><errorCode>";
					final String endTag = "</errorDesc></error>";
					int begin = response.indexOf(beginTag);
					int end = response.indexOf(endTag);
					String errMsg = response.substring(begin + beginTag.length(), end);
					errMsg = errMsg.replace("</errorCode><errorDesc>", "");
					
					sampleResult.setSuccessful(Boolean.FALSE);
					sampleResult.setResponseCodeOK();
		            sampleResult.setResponseMessage(errMsg);					
				}
				else {
					
					sampleResult.setSuccessful(Boolean.TRUE);
					sampleResult.setResponseCodeOK();
		            sampleResult.setResponseMessage(response);
				}
			}
			
			rs.close();
			stmt.close();
		}
		catch(Throwable ex) {
			
            sampleResult.sampleEnd();
            sampleResult.setSuccessful(Boolean.FALSE);
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessage(ex.getMessage());
            
			LOGGER.error("Error message: ", ex);
		}
		
		return sampleResult;
	}
	
	private String readResource(String resourceName) throws URISyntaxException, IOException {
		
		LOGGER.info("ProfileXmlSampler.readResource(\"" +  resourceName + "\")");
		
		URI fileURI = getClass().getResource(resourceName).toURI();
		Utils.initFileSystem(fileURI);
		
		Path path = Paths.get(fileURI);       
		byte[] fileBytes = Files.readAllBytes(path);

		LOGGER.info("ProfileXmlSampler.readResource results: " +  new String(fileBytes));
		
		return new String(fileBytes);
	}	
}
