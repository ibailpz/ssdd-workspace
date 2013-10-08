package es.deusto.ingenieria.ssdd.networking.tcp;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPServer {
	private static final int DEFAULT_PORT = 7896;
	
	public static void main(String args[]) {
		//args[0] = Server socket port
		int serverPort = args.length == 0 ? TCPServer.DEFAULT_PORT : Integer.parseInt(args[0]);
		int clientCount = 0;
		
		try (ServerSocket tcpServerSocket = new ServerSocket(serverPort);) {
			System.out.println(" - Waiting for connections '" + 
		                           tcpServerSocket.getInetAddress().getHostAddress() + ":" + 
		                           tcpServerSocket.getLocalPort() + "' ...");
			
			while (clientCount++ < 3) {				
				new EchoService(tcpServerSocket.accept());
			}
		} catch (IOException e) {
			System.err.println("# TCPServer IO error:" + e.getMessage());
		}
	}
}