package es.deusto.ingenieria.ssdd.chat.process;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ProcessThread extends Thread {

	private static HashMap<String, User> users = new HashMap<>();
	private DatagramPacket message;

	public ProcessThread(DatagramPacket message) {
		this.message = message;
	}

	@Override
	public void run() {
		String command = new String(message.getData());
		String[] split = command.split(" ");
		User user = users.get(split[1]);
		if (user == null) {
			if (split[0].toLowerCase().equals("connect")) {
				if (users.containsKey(split[1])) {
					sendMessage("error_nick " + split[1] + " in use",
							this.message.getAddress().getHostAddress(),
							this.message.getPort());
				} else {
					User u = new User(this.message.getAddress()
							.getHostAddress(), this.message.getPort());
					u.setNick(split[1]);
					users.put(split[1], u);
					// TODO Send user list update
				}
			} else {
				sendMessage("error_restart", this.message.getAddress()
						.getHostAddress(), this.message.getPort());
			}
		} else {
			if (split[0].toLowerCase().equals("disconnect")) {
				disconnect(user);
			} else {
				switch (user.getState()) {
				case 1:
					if (split[0].toLowerCase().equals("send_invitation")) {
						User user2 = users.get(split[2]);
						if (user2 == null) {
							sendMessage("error_restart", this.message
									.getAddress().getHostAddress(),
									this.message.getPort());
						} else {

						}
					}
				case 2:
					if (split[0].toLowerCase().equals("cancel_invitation")) {

					}
				case 3:
					if (split[0].toLowerCase().equals("send_message")) {

					} else if (split[0].toLowerCase().equals("close_chat")) {

					} else if (split[0].toLowerCase().equals("send_invitation")) {

					}
				}
			}
		}
	}

	private void disconnect(User u) {
		users.remove(u.getNick());
		// TODO Send user list update
	}

	private void sendMessage(String message, String serverIP, int serverPort) {
		// String serverIP = this.message.getAddress().getHostAddress();
		// int serverPort = this.message.getPort();

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
