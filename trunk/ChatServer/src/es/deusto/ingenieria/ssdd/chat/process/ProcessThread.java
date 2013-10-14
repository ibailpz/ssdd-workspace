package es.deusto.ingenieria.ssdd.chat.process;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
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
		command = command.trim();
		String[] split = command.split(" ");
		User user = users.get(split[1]);

		if (user == null) {
			if (split[0].toLowerCase().equals("connect")) {
//				if (users.containsKey(split[1])) {
//					sendMessage("error_nick " + split[1] + " in use",
//							this.message.getAddress().getHostAddress(),
//							Integer.parseInt(split[2]));
//				} else {
					User u = new User(this.message.getAddress()
							.getHostAddress(), Integer.parseInt(split[2]));
					u.setNick(split[1]);
					users.put(split[1], u);
					updateUserList();
//				}
			} else {
				errorRestart(user);
			}
		} else {
			if (split[0].toLowerCase().equals("connect")) {
				sendMessage("error_nick " + split[1] + " in use",
				this.message.getAddress().getHostAddress(),
				Integer.parseInt(split[2]));
			} else if (split[0].toLowerCase().equals("accept")) {
				User userOther = users.get(split[2]);
				if (userOther == null) {
					sendMessage("error_user", user.getIp(), user.getPort());
					updateUserList();
				} else {
					sendMessage("accept " + user.getNick(), userOther.getIp(),
							userOther.getPort());
					user.setState(3);
					userOther.setState(3);
				}
			} else if (split[0].toLowerCase().equals("busy")) {
				User userOther = users.get(split[2]);
				if (userOther == null) {
					sendMessage("error_user", user.getIp(), user.getPort());
					updateUserList();
				} else {
					sendMessage("busy " + user.getNick(), userOther.getIp(),
							userOther.getPort());
					userOther.goBackState();
				}
			} else {
				switch (user.getState()) {
				case 1:
					if (split[0].toLowerCase().equals("disconnect")) {
						users.remove(user.getNick());
						updateUserList();
					} else if (split[0].toLowerCase().equals("send_invitation")) {
						sendInvitation(split[2], user);
					} else {
						errorRestart(user);
					}
					break;
				case 2:
					if (split[0].toLowerCase().equals("cancel_invitation")) {
						User userOther = users.get(split[2]);
						if (userOther != null) {
							sendMessage("cancel_invitation " + user.getNick(),
									userOther.getIp(), userOther.getPort());
							user.setState(1);
						}
					} else {
						errorRestart(user);
					}
					break;
				case 3:
					if (split[0].toLowerCase().equals("send_message")) {
						User userOther = users.get(split[2]);
						if (userOther == null) {
							sendMessage("error_user", user.getIp(),
									user.getPort());
							updateUserList();
						} else {
							String message = command.substring((split[0]
									.length()
									+ split[1].length()
									+ split[2].length() + 3));
							sendMessage("receive_message " + user.getNick()
									+ " " + message, userOther.getIp(),
									userOther.getPort());
						}
					} else if (split[0].toLowerCase().equals("close_chat")) {
						User userOther = users.get(split[2]);
						closeChat(user, userOther);

					} else if (split[0].toLowerCase().equals("send_invitation")) {
						sendInvitation(split[2], user);
					} else {
						errorRestart(user);
					}
					break;
				}
			}
		}
	}

	private void closeChat(User user, User userOther) {
		if (userOther == null) {
			sendMessage("error_user", user.getIp(), user.getPort());
			updateUserList();
		} else {
			sendMessage("close_chat " + user.getNick(), userOther.getIp(),
					userOther.getPort());
			user.setState(1);
			userOther.setState(1);
		}
	}

	private void errorRestart(User user) {
		sendMessage("error_restart", user.getIp(), user.getPort());
		user.setState(1);
	}

	private void sendInvitation(String u2, User user) {
		User userOther = users.get(u2);
		if (userOther == null) {
			sendMessage("error_user " + u2 + " does not exist", user.getIp(),
					user.getPort());
			updateUserList();
		} else {
			sendMessage("invitation " + user.getNick(), userOther.getIp(),
					userOther.getPort());
			user.setState(2);
		}
	}

	private void updateUserList() {
		ArrayList<String> userNames = new ArrayList<>(users.keySet());
		for (User u : users.values()) {
			StringBuilder sb = new StringBuilder();
//			userNames.remove(u.getNick());
			for (int i = 0; i < userNames.size(); i++) {
				if (!userNames.get(i).equals(u.getNick())) {
					sb.append(userNames.get(i)).append("||");
				}
			}
			String users = sb.toString();
			if (sb.length() > 2) {
				users = sb.toString().substring(0, sb.length() - 2);
			}
			sendMessage("update_users " + users, u.getIp(), u.getPort());
//			userNames.add(u.getNick());
		}
	}

	private void sendMessage(String message, String serverIP, int port) {
		// String serverIP = this.message.getAddress().getHostAddress();
		// int serverPort = this.message.getPort();

		try (DatagramSocket udpSocket = new DatagramSocket()) {
			InetAddress host = InetAddress.getByName(serverIP);
			byte[] byteMsg = message.getBytes();
			DatagramPacket request = new DatagramPacket(byteMsg,
					byteMsg.length, host, port);
			udpSocket.send(request);
			System.out.println(" - Sent a request to '" + host.getHostAddress()
					+ ":" + request.getPort() + "' -> "
					+ new String(request.getData()));

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
