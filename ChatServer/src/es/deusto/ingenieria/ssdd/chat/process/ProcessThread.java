package es.deusto.ingenieria.ssdd.chat.process;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ProcessThread extends Thread {

	private DatagramPacket message;

	public ProcessThread(DatagramPacket message) {
		this.message = message;
	}

	@Override
	public void run() {
		String command = new String(message.getData());
		String[] split = command.split(" ");
		if (split[0].toLowerCase().equals("connect")) {
			
		} else if (split[0].toLowerCase().equals("disconnect")) {

		} else if (split[0].toLowerCase().equals("send_invitation")) {

		} else if (split[0].toLowerCase().equals("cancel_invitation")) {

		} else if (split[0].toLowerCase().equals("send_message")) {

		} else if (split[0].toLowerCase().equals("close_chat")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		} else if (split[0].toLowerCase().equals("connect")) {

		}
	}

	private void sendMessage(String message) {
		String serverIP = this.message.getAddress().getHostAddress();
		int serverPort = this.message.getPort();

		try (DatagramSocket udpSocket = new DatagramSocket()) {
			InetAddress serverHost = InetAddress.getByName(serverIP);
			byte[] byteMsg = message.getBytes();
			DatagramPacket request = new DatagramPacket(byteMsg,
					byteMsg.length, serverHost, serverPort);
			udpSocket.send(request);
			System.out.println(" - Sent a request to '"
					+ serverHost.getHostAddress() + ":" + request.getPort()
					+ "' -> " + new String(request.getData()));

			byte[] buffer = new byte[1024];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			udpSocket.receive(reply);
			System.out.println(" - Received a reply from '"
					+ reply.getAddress().getHostAddress() + ":"
					+ reply.getPort() + "' -> " + new String(reply.getData()));
		} catch (SocketException e) {
			System.err.println("# UDPClient Socket error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("# UDPClient IO error: " + e.getMessage());
		}
	}
}
