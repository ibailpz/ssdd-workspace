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
				errorRestart();
			}
		} else {
			if (split[0].toLowerCase().equals("disconnect")) {
				disconnect(user);
			} else {
				switch (user.getState()) {
				case 1:
					if (split[0].toLowerCase().equals("send_invitation")) {
						sendInvitation(split[2], user);
					} else {
						errorRestart();
					}
					break;
				case 2:
					if (split[0].toLowerCase().equals("cancel_invitation")) {
						User user2 = users.get(split[2]);
						if (user2 == null) {
							errorRestart();
						} else {
							sendMessage("cancel_invitation " + user.getNick(),
									user2.getIp(), user2.getPort());
							user.setState(1);
						}
					} else {
						errorRestart();
					}
					break;
				case 3:
					if (split[0].toLowerCase().equals("send_message")) {
						User user_other = users.get(split[2]);
						if (user_other == null){
							sendMessage("error_user", user.getIp(), user.getPort());
							//TODO update user list
						}
						else{
							String message = command.substring((split[0].length() + split[1].length() + split[2].length()+3));
							sendMessage("receive_message "+user.getNick()+" "+message, user_other.getIp(), user_other.getPort());
						}
						
					} else if (split[0].toLowerCase().equals("close_chat")) {
						User user_other = users.get(split[2]);
						close_chat(user, user_other);
											
					} else if (split[0].toLowerCase().equals("send_invitation")) {
						
					} else {
						errorRestart();
					}
					break;
				}
			}
		}
	}
	
	private void close_chat(User user, User user_other){
		if (user_other == null){
			sendMessage("error_user", user.getIp(), user.getPort());
			//TODO update user list
		}
		else{
			sendMessage("close_chat "+user.getNick(), user_other.getIp(), user_other.getPort());
			users.get(user.getNick()).setState(1);
			users.get(user_other.getNick()).setState(1);
		}
	}

	private void errorRestart() {
		sendMessage("error_restart",
				this.message.getAddress().getHostAddress(),
				this.message.getPort());
	}

	private void sendInvitation(String u2, User user) {
		User user2 = users.get(u2);
		if (user2 == null) {
			errorRestart();
		} else {
			sendMessage("invitation " + user.getNick(), user2.getIp(),
					user2.getPort());
			user.setState(2);
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
