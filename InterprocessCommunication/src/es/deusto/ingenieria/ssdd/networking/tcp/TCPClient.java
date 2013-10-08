package es.deusto.ingenieria.ssdd.networking.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


public class TCPClient {
	private static final String DEFAULT_IP = "0.0.0.0";
	private static final int DEFAULT_PORT = 7896;
	private static final String DEFAULT_MESSAGE = "Hello World!";

	public static void main(String args[]) {
		// args[0] = Server IP
		String serverIP = args.length == 0 ? TCPClient.DEFAULT_IP: args[0];
		// args[1] = Server socket port
		int serverPort = args.length == 0 ? TCPClient.DEFAULT_PORT : Integer.parseInt(args[1]);
		// argrs[2] = Message
		String message = args.length == 0 ? TCPClient.DEFAULT_MESSAGE: args[2];

		try (Socket tcpSocket = new Socket(serverIP, serverPort);
		     DataInputStream in = new DataInputStream(tcpSocket.getInputStream());
			 DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream())){
			
			out.writeUTF(message);
			System.out.println(" - Sent data to '" + tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getPort() + 
                               "' -> '" + message + "'");
						
			String data = in.readUTF();			
			System.out.println(" - Received data from '" + tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getPort() + 
                               "' -> '" + data + "'");
		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
		}
	}
}