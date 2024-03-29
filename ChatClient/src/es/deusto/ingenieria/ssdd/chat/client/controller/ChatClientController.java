package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {

	private String serverIP;
	private int serverPort;
	private int localPort;
	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;
	private Thread process;

	public ChatClientController() {
		this.serverIP = null;
		this.serverPort = -1;
	}

	private void processRequest(DatagramPacket request) {
		String message = new String(request.getData()).trim();
		String[] split = message.split(" ");

		if (split[0].equals("error_nick")) {
			connectedUser = null;
			this.observable.onError("ERROR nick");
		} else {
			this.observable.onConnect(true);
			if (split[0].equals("receive_message")) {
				receiveMessage(message.substring(split[0].length()
						+ split[1].length() + 2));
			} else if (split[0].equals("invitation")) {
				receiveChatRequest(split[1]);
			} else if (split[0].equals("update_users")) {
				List<String> users = new ArrayList<>();
				if (split.length > 1) {
					String[] splitUsers = split[1].split("\\|\\|");
					for (int i = 0; i < splitUsers.length; i++) {
						users.add(splitUsers[i]);
					}
				}
				this.observable.onUsersUpdated(users);
			} else if (split[0].equals("close_chat")) {
				receiveChatClosure();
			} else if (split[0].equals("accept")) {
				this.chatReceiver = new User();
				this.chatReceiver.setNick(split[1]);
				this.observable.onChatRequestResponse(split[1], true);
			} else if (split[0].equals("busy")) {
				this.observable.onChatRequestResponse(split[1], false);
			} else if (split[0].equals("error_user")) {
				this.observable.onError("ERROR user");
			} else if (split[0].equals("cancel_invitation")) {
				this.observable.onInvitationCancelled(split[1]);
			} else if (split[0].equals("error_restart")) {
				this.observable.onError("RESTART");
			}
		}
	}

	public String getConnectedUser() {
		if (this.connectedUser != null) {
			return this.connectedUser.getNick();
		} else {
			return null;
		}
	}

	public String getChatReceiver() {
		if (this.chatReceiver != null) {
			return this.chatReceiver.getNick();
		} else {
			return null;
		}
	}

	public String getServerIP() {
		return this.serverIP;
	}

	public int gerServerPort() {
		return this.serverPort;
	}

	public boolean isConnected() {
		return this.connectedUser != null;
	}

	public boolean isChatSessionOpened() {
		return this.chatReceiver != null;
	}

	public void addLocalObserver(MessageReceiverInterface observer) {
		this.observable = observer;
	}

	public void deleteLocalObserver() {
		this.observable = null;
	}

	public void connect(String ip, int serverPort, int localPort, String nick)
			throws IOException {
		this.connectedUser = new User();
		this.connectedUser.setNick(nick);
		this.serverIP = ip;
		this.serverPort = serverPort;
		this.localPort = localPort;
		if (process == null) {
			process = new Thread() {

				@Override
				public void run() {
					for (; !this.isInterrupted();) {
						try (DatagramSocket udpSocket = new DatagramSocket(
								ChatClientController.this.localPort)) {
							for (; !this.isInterrupted();) {
								byte[] buffer = new byte[1024];

								System.out
										.println(" - Waiting for connections on port '"
												+ ChatClientController.this.localPort
												+ "' ...");

								final DatagramPacket request = new DatagramPacket(
										buffer, buffer.length);
								udpSocket.receive(request);
								System.out
										.println(" - Received a request from '"
												+ request.getAddress()
														.getHostAddress() + ":"
												+ request.getPort() + "' -> "
												+ new String(request.getData()));

								new Thread(new Runnable() {

									@Override
									public void run() {
										processRequest(request);
									}
								}, "ProcessRequest").start();
							}
						} catch (SocketException e) {
							System.err.println("# UDPServer Socket error: "
									+ e.getMessage());
						} catch (IOException e) {
							System.err.println("# UDPServer IO error: "
									+ e.getMessage());
						}
					}
				}
			};
			process.start();
		}
		sendCommand("connect " + nick + " " + this.localPort);
	}

	public void disconnect() {
		try {
			sendCommand("disconnect " + connectedUser.getNick());
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.connectedUser = null;
		this.chatReceiver = null;
		// if (process != null) {
		// process.interrupt();
		// }
	}

	public void sendMessage(String message) throws IOException {
		sendCommand("send_message " + connectedUser.getNick() + " "
				+ chatReceiver.getNick() + " " + message);
	}

	public void receiveMessage(String message) {
		// Notify the received message to the GUI
		this.observable.onMessageReceived(message, chatReceiver.getNick());
	}

	public void sendChatRequest(String to) throws IOException {
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		sendCommand("send_invitation " + connectedUser.getNick() + " " + to);
	}

	public void receiveChatRequest(String user) {
		// Notify the chat request details to the GUI
		this.observable.onChatInvitationReceived(user);
	}

	public void cancelInvitation(String otherUser) throws IOException {
		sendCommand("cancel_invitation " + connectedUser.getNick() + " "
				+ otherUser);
	}

	public void acceptChatRequest(String user) throws IOException {
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		this.chatReceiver = new User();
		this.chatReceiver.setNick(user);
		sendCommand("accept " + connectedUser.getNick() + " " + user);
	}

	public void refuseChatRequest(String user) throws IOException {
		sendCommand("busy " + connectedUser.getNick() + " " + user);
	}

	public void sendChatClosure() throws IOException {
		sendCommand("close_chat " + connectedUser.getNick() + " "
				+ chatReceiver.getNick());
		this.chatReceiver = null;
	}

	public void receiveChatClosure() {
		// Notify the chat request details to the GUI
		this.observable.onChatDisconnect(chatReceiver.getNick());
		chatReceiver = null;
	}

	private void sendCommand(String command) throws IOException {
		try (DatagramSocket udpSocket = new DatagramSocket()) {
			InetAddress serverHost = InetAddress.getByName(serverIP);
			byte[] byteMsg = command.getBytes();
			DatagramPacket request = new DatagramPacket(byteMsg,
					byteMsg.length, serverHost, serverPort);
			udpSocket.send(request);
			System.out.println(" - Sent a request to '"
					+ serverHost.getHostAddress() + ":" + request.getPort()
					+ "' -> " + new String(request.getData()));

			byte[] buffer = new byte[1024];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			udpSocket.setSoTimeout(1);
			udpSocket.receive(reply);
			System.out.println(" - Received a reply from '"
					+ reply.getAddress().getHostAddress() + ":"
					+ reply.getPort() + "' -> " + new String(reply.getData()));
		} catch (SocketTimeoutException e) {
			// Do nothing
		}
	}
}