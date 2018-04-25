package com.psldebugger.jmeter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jpos.iso.ISOMsg;

public class IsoTemplate {
	
	public String Random(int length) {
		
		Random r = new Random();
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        while (counter++ < length) sb.append(r.nextInt(9));
        return sb.toString();
	}
	
	// yyyy-MM-dd HH:mm:ss.SSS
	static private DateFormat shortDateFormat = new SimpleDateFormat("MMdd");
	static private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	
	public String CurrentDate(String format) {
		
		if (format.equals("MMDD"))
			return shortDateFormat.format(new Date());
		else
			if (format.equals("YEARMMDD"))
				return dateFormat.format(new Date());
			else
				throw new RuntimeException("Incorrect format: " + format);
	}

	// yyyy-MM-dd HH:mm:ss.SSS
	static private DateFormat timeFormat = new SimpleDateFormat("HHmmss");
	
	public String CurrentTime(String format) {
		
		if (format.equals("2460SS"))
			format = "HHmmss";
		else
			throw new RuntimeException("Incorrect format: " + format);		
		
		return timeFormat.format(new Date());
	}
	
	private static void initFileSystem(URI uri) throws IOException {
		
	   for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {		   
	        if (provider.getScheme().equalsIgnoreCase("jar")) {
	            try {
	                provider.getFileSystem(uri);
	            } catch (FileSystemNotFoundException e) {
	                // in this case we need to initialize it first:
	                provider.newFileSystem(uri, Collections.emptyMap());
	            }
	        }
	    }
	}	
	
	private final static Pattern funcPattern = Pattern.compile("%(\\w+)\\((\\w+)\\)");	
	String calculateFunction(String fs) {
	
		Matcher funcMatcher = funcPattern.matcher(fs);
		if (funcMatcher.matches()) {
			
			String func = funcMatcher.group(1);
			String param = funcMatcher.group(2);
			switch (func) {
			case "CurrentDate": return CurrentDate(param);
			case "CurrentTime": return CurrentTime(param);
			case "Random": return Random(Integer.valueOf(param));
			default: throw new RuntimeException("Unknown function: " + fs);
			}
		}
		else
			throw new RuntimeException("Can't parse function: " + fs);
	}
	
	static private ConcurrentMap<String, IsoTemplate> templateMap = new ConcurrentHashMap<String, IsoTemplate>();
	static public ISOMsg createMessage(String resourceName) throws URISyntaxException, IOException {
		
		if (!templateMap.containsKey(resourceName)) {
			
			synchronized(templateMap ) {
		
				if (!templateMap.containsKey(resourceName)) {
					
					if (templateMap.size() == 0) {
						
						URL url = IsoTemplate.class.getResource(ISO_PATH);
						initFileSystem(url.toURI());
					}

					IsoTemplate newTemplate = new IsoTemplate();
					newTemplate.loadResources(resourceName, true);
					templateMap.put(resourceName, newTemplate);
				}
			}
		}
				
		IsoTemplate template = templateMap.get(resourceName);
		return template.createMessage();
	}
	
	final static String ISO_PATH = "/iso/"; 
	
	/*
	 * Parsing one of these blocks:
	 * #MESSAGE
	 * #RUNTIME
	 */
	enum State { TOP, MESSAGE, RUNTIME };
	
	static private final Pattern includePattern = Pattern.compile("^#INCLUDE\\s+<(\\w+\\.\\w+)>\\s*(;.*)?");
	static private final Pattern beginDomainPattern = Pattern.compile("^#DOMAIN\\s+<(\\w+)>:?\\s*(;.*)?");
	static private final Pattern endPattern = Pattern.compile("^#END\\s*(;.*)?");
	static private final Pattern contentPattern = Pattern.compile("\\s+<(\\w+)>\\s*=\\s*<(\\w+)>\\s*(;.*)?");
	static private final Pattern valuePattern = Pattern.compile("#VALUE\\s+<(\\w+)>\\s*=\\s*<(.+)>\\s*(;.*)?");
	static private final Pattern refPattern = Pattern.compile("\\[(.+?)\\]");
	
	private List<String> lines;
	
	public Map<String, String> loadResources(String resourceName, boolean storeLines) throws URISyntaxException, IOException {
		
		URL url = IsoTemplate.class.getResource(ISO_PATH + resourceName);
		Path path = Paths.get(url.toURI());       
		List<String> fileLines = Files.readAllLines(path);
		if (storeLines)
			lines = fileLines;			
		
		return parseResources(fileLines);
	}
	
	/* parseValue calculates expressions like this:
	 * #VALUE <CANL1> = <[TERM_OWNER][TERM_CITY][TERM_STATE][TERM_COUNTRY][TERM_ADDRESS][TERM_BRANCH][TERM_REGION][TERM_CLASS]>
	 */
	void parseValue(Matcher matcher, Map<String, String> map) {
		
		String field = matcher.group(1);
		String value = matcher.group(2);
		
		if (value.startsWith("[")) {

			StringBuilder sb = new StringBuilder(); 
			Matcher refMatcher = refPattern.matcher(value);
			while (refMatcher.find()) {
			
				String key = refMatcher.group(1);
				if (key.startsWith("%")) {
				
					sb.append(calculateFunction(key));
				}
				else								
					if (resourceMap.containsKey(key)) {
						
						sb.append(resourceMap.get(key));
					}
					else
						throw new RuntimeException("Can't find the definition. Key: " + key + ". Line: " + matcher.group(0));							
			}
			map.put(field, sb.toString());
		}
		else
			map.put(field, value);						
	}
	
	// Every IsoTemplate will load #DOMAIN and #VALUE definitions into resourceMap.
	// resourceMap is populated in parseResources. 
	private Map<String, String> resourceMap = new HashMap<String, String>();
	
	Map<String, String> parseResources(List<String> lines) throws URISyntaxException, IOException {
				
		String domain = null;
		State state = State.TOP;
				
		for (String line: lines) {

			// Comment
			if (line.startsWith(";") || line.isEmpty())
				continue;
			
			if (domain == null) {

				Matcher beginMessageMatcher = beginMessagePattern.matcher(line);
				if (beginMessageMatcher.matches()) {
					
					state = State.MESSAGE;
					continue;
				}
				
				Matcher runtimeMatcher = runtimePattern.matcher(line);
				if (runtimeMatcher.matches()) {
					
					state = State.RUNTIME;
					continue;
				}				
				
				Matcher endMatcher = endPattern.matcher(line);
				if (endMatcher.matches()) {
					
					state = State.TOP;
					continue;
				}
				
				if (state != State.TOP)
					continue;
				
				Matcher beginDomainMatcher = beginDomainPattern.matcher(line);
				if (beginDomainMatcher.matches()) {
					
					domain = beginDomainMatcher.group(1);
					continue;
				}

				Matcher includeMatcher = includePattern.matcher(line);
				if (includeMatcher.matches()) {
					
					String fileName = includeMatcher.group(1);
					loadResources(fileName, false);
					continue;
				}
				
				Matcher valueMatcher = valuePattern.matcher(line);
				if (valueMatcher.matches()) {

					parseValue(valueMatcher, resourceMap);
				}
				else
					throw new RuntimeException("Patterns don't match: " + line);
			}
			else {
			
				Matcher matcher = contentPattern.matcher(line);
				if (matcher.matches()) {
					
					String field = matcher.group(1);
					String value = matcher.group(2);
					resourceMap.put(domain + "." + field, value);
				}
				else {
					
					// #END
					Matcher endDomainMatcher = endPattern.matcher(line);
					if (endDomainMatcher.matches()) {
						domain = null;
						continue;
					}
					else
						throw new RuntimeException("Patterns don't match. Domain:  " + domain + ", line: " + line);					
				}
			}
		};
		
		return resourceMap;
	}
	
	static private final Pattern beginMessagePattern = Pattern.compile("^#MESSAGE <(\\w+)>");
	static private final Pattern messageContentPattern = Pattern.compile("\\s+<(\\w+)>\\s*=\\s*<(.+)>\\s*(;.*)?");
	static private final Pattern runtimePattern = Pattern.compile("#RUNTIME");	
	
	public ISOMsg createMessage() {
		
		Map<String, String> messageMap = parseMessage(lines);
		return mapToMsg(messageMap);
	}
	
	Map<String, String> parseMessage(List<String> lines) {
		
		State state = State.TOP;
		Map<String, String> messageMap = new HashMap<String, String>(); 
		
		for (String line: lines) {

			if (line.trim().startsWith(";") || line.isEmpty())
				continue;
			
			if (state == State.TOP) {

				Matcher beginMessageMatcher = beginMessagePattern.matcher(line);
				if (beginMessageMatcher.matches()) {
					
					state = State.MESSAGE;
					continue;
				}
				
				Matcher runtimeMatcher = runtimePattern.matcher(line);
				if (runtimeMatcher.matches()) {
					
					state = State.RUNTIME;
					continue;
				}
								
				Matcher includeMatcher = includePattern.matcher(line);
				if (includeMatcher.matches()) 
					continue;

				Matcher valueMatcher = valuePattern.matcher(line);
				if (valueMatcher.matches()) 
					continue;
				
				throw new RuntimeException("Can't parse this line: " + line);
			}
			
			if (state == State.MESSAGE) {

				Matcher valueMatcher = messageContentPattern.matcher(line);
				if (valueMatcher.matches()) {
					
					parseValue(valueMatcher, messageMap);
					continue;
				}
				else
				{
					Matcher endMatcher = endPattern.matcher(line);
					if (endMatcher.matches()) {
						
						state = State.TOP;
						continue;
					}
					else
						throw new RuntimeException("Can't parse message content: " + line);					
				}				
			}
			
			if (state == State.RUNTIME) {
				
				Matcher endMatcher = endPattern.matcher(line);
				if (endMatcher.matches()) {
					
					state = State.TOP;
					continue;
				}				
			}
		}
		
		return messageMap;		
	}
	
	ISOMsg mapToMsg(Map<String, String> messageMap) {
		
		ISOMsg msg = new ISOMsg(messageMap.get("MID"));		
		for (Entry<String, String> entry: messageMap.entrySet()) {
			
			if (entry.getKey().startsWith("F")) {
							
				String fieldNumber = entry.getKey().substring(1);
				//System.err.println(fieldNumber + " = " + entry.getValue());
				msg.set(fieldNumber, entry.getValue());
			}			
		}
		
		return msg;
	}
}
