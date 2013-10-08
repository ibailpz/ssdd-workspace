package es.deusto.ingenieria.ssdd.networking.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPClient {
	private static final String DEFAULT_IP = "0.0.0.0";
	private static final int DEFAULT_PORT = 6789;
	private static final String DEFAULT_MESSAGE = "Hello World!";	
	
	public static void main(String args[]) {
		//args[0] = Server IP
		String serverIP = args.length == 0 ? UDPClient.DEFAULT_IP : args[0];
		//args[1] = Server socket port
		int serverPort = args.length == 0 ? UDPClient.DEFAULT_PORT : Integer.parseInt(args[1]);
		//args[2] = Message
		String message = args.length == 0 ? UDPClient.DEFAULT_MESSAGE : args[2];
		
		try (DatagramSocket udpSocket = new DatagramSocket()) {
			InetAddress serverHost = InetAddress.getByName(serverIP);			
			byte[] byteMsg = message.getBytes();
			DatagramPacket request = new DatagramPacket(byteMsg, byteMsg.length, serverHost, serverPort);
			udpSocket.send(request);
			System.out.println(" - Sent a request to '" + serverHost.getHostAddress() + ":" + request.getPort() + 
					           "' -> " + new String(request.getData()));
			
			byte[] buffer = new byte[1024];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			udpSocket.receive(reply);			
			System.out.println(" - Received a reply from '" + reply.getAddress().getHostAddress() + ":" + reply.getPort() + 
					           "' -> "+ new String(reply.getData()));
		} catch (SocketException e) {
			System.err.println("# UDPClient Socket error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("# UDPClient IO error: " + e.getMessage());
		}
	}
}