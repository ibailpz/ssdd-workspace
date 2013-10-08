package es.deusto.ingenieria.ssdd.networking.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class URLReader {
	
	private static final String DEFAULT_URL = "http://www.google.es";
	
	public static void main(String[] args) {
		// args[0] = Server URL
		String url = args.length == 0 ? URLReader.DEFAULT_URL : args[0];
		URLConnection urlConn = null;		
		
		try {
			//Open URL Connection
			urlConn = new URL(url).openConnection();
		} catch (MalformedURLException e) {
			System.err.println("# URLConnection MalformedURL error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# URLConnection IO error: " + e.getMessage());
		}
				
		//Read Server Response
		try (InputStream inStream = urlConn.getInputStream();
			 Scanner in = new Scanner(inStream)) {
			while (in.hasNextLine()) {
				System.out.println(in.nextLine());
			}
		} catch (IOException e) {
			System.err.println("# URLConnection IO error: " + e.getMessage());
		}
	}
}
