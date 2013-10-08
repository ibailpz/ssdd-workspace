package es.deusto.ingenieria.ssdd.networking.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class MulticastPeer {
	private static final String DEFAULT_IP = "228.5.6.7";
	private static final int DEFAULT_PORT = 6789;
	private static final String DEFAULT_MESSAGE = "Hello World!";	
	
	public static void main(String args[]) {
		// args[0] = Server IP
		String groupIP = args.length == 0 ? MulticastPeer.DEFAULT_IP: args[0];
		// args[1] = Server socket port
		int port = args.length == 0 ? MulticastPeer.DEFAULT_PORT : Integer.parseInt(args[1]);
		// argrs[2] = Message
		String message = args.length == 0 ? MulticastPeer.DEFAULT_MESSAGE: args[2];

		try (MulticastSocket socket = new MulticastSocket(port)) {
			InetAddress group = InetAddress.getByName(groupIP);
			socket.joinGroup(group);
			
			DatagramPacket messageOut = new DatagramPacket(message.getBytes(), message.length(), group, port);
			socket.send(messageOut);
			
			System.out.println(" - Sent a message to '" + messageOut.getAddress().getHostAddress() + ":" + messageOut.getPort() + 
			                   "' -> " + new String(messageOut.getData()));
			
			byte[] buffer = new byte[1024];			
			DatagramPacket messageIn = null;
			
			for (int i = 0; i < 3; i++) { // get messages from other 2 peers in the same group
				messageIn = new DatagramPacket(buffer, buffer.length);
				socket.receive(messageIn);

				System.out.println(" - Received a message from '" + messageIn.getAddress().getHostAddress() + ":" + messageIn.getPort() + 
		                   		   "' -> " + new String(messageIn.getData()));
			}
			
			socket.leaveGroup(group);
		} catch (SocketException e) {
			System.err.println("# Socket Error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# IO Error: " + e.getMessage());
		}
	}
}