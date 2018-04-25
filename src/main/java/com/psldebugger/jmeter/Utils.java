package com.psldebugger.jmeter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;

public class Utils {
	
	public static void initFileSystem(URI uri) throws IOException {
		
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
}
