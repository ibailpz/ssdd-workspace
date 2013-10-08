package es.deusto.ingenieria.ssdd.networking.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer {
	private static final int PORT = 6789;
	
	public static void main(String args[]) {
		//args[0] = Server socket port
		int serverPort = args.length == 0 ? UDPServer.PORT : Integer.parseInt(args[0]);
		int clientCount = 0;
		
		try (DatagramSocket udpSocket = new DatagramSocket(serverPort)) {
			DatagramPacket request = null;
			DatagramPacket reply = null;
			byte[] buffer = new byte[1024];
			
			System.out.println(" - Waiting for connections '" + 
			                       udpSocket.getLocalAddress().getHostAddress() + ":" + 
					               serverPort + "' ...");
			
			while (clientCount++ < 3) {
				request = new DatagramPacket(buffer, buffer.length);
				udpSocket.receive(request);				
				System.out.println(" - Received a request from '" + request.getAddress().getHostAddress() + ":" + request.getPort() + 
				                   "' -> " + new String(request.getData()));
				
				reply = new DatagramPacket(request.getData(), request.getLength(), request.getAddress(), request.getPort());
				udpSocket.send(reply);				
				System.out.println(" - Sent a reply to '" + reply.getAddress().getHostAddress() + ":" + reply.getPort() + 
						           "' -> " + new String(reply.getData()));
			}
		} catch (SocketException e) {
			System.err.println("# UDPServer Socket error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# UDPServer IO error: " + e.getMessage());
		}
	}
}
