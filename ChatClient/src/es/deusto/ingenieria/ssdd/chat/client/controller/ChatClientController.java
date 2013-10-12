package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {
	private String serverIP;
	private int serverPort;
	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;

	public ChatClientController() {
		this.serverIP = null;
		this.serverPort = -1;
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (;;) {
					try (DatagramSocket udpSocket = new DatagramSocket(
							serverPort)) {
						byte[] buffer = new byte[1024];

						System.out.println(" - Waiting for connections '"
								+ udpSocket.getLocalAddress().getHostAddress()
								+ ":" + serverPort + "' ...");

						final DatagramPacket request = new DatagramPacket(
								buffer, buffer.length);
						udpSocket.receive(request);
						System.out.println(" - Received a request from '"
								+ request.getAddress().getHostAddress() + ":"
								+ request.getPort() + "' -> "
								+ new String(request.getData()));

						new Thread(new Runnable() {

							@Override
							public void run() {
								processRequest(request);
							}
						}, "ProcessRequest");
					} catch (SocketException e) {
						System.err.println("# UDPServer Socket error: "
								+ e.getMessage());
					} catch (IOException e) {
						System.err.println("# UDPServer IO error: "
								+ e.getMessage());
					}
				}
			}
		}, "ReceivingThread");
	}

	private void processRequest(DatagramPacket request) {
		String message = new String(request.getData());
		String[] split = message.split(" ");
		if (split[0].equals("")) {
			
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

	public void connect(String ip, int port, String nick) {

		// ENTER YOUR CODE TO CONNECT

		this.connectedUser = new User();
		this.connectedUser.setNick(nick);
		this.serverIP = ip;
		this.serverPort = port;
	}

	public void disconnect() {
		try {
			sendCommand("disconnect " + connectedUser.getNick());
		} catch (IOException e) {
			e.printStackTrace();
			// TODO Display error message
		}

		this.connectedUser = null;
		this.chatReceiver = null;
	}

//	public List<String> getConnectedUsers() {
//		List<String> connectedUsers = new ArrayList<>();
//
//		// ENTER YOUR CODE TO OBTAIN THE LIST OF CONNECTED USERS
//		connectedUsers.add("Default");
//
//		return connectedUsers;
//	}

	public void sendMessage(String message) throws IOException {
		sendCommand("");
		// ENTER YOUR CODE TO SEND A MESSAGE
	}

	public void receiveMessage() {

		// ENTER YOUR CODE TO RECEIVE A MESSAGE

		String message = "Received message";

		// Notify the received message to the GUI
		this.observable.onMessageReceived(message, chatReceiver.getNick());
	}

	public void sendChatRequest(String to) {

		// ENTER YOUR CODE TO SEND A CHAT REQUEST

		this.chatReceiver = new User();
		this.chatReceiver.setNick(to);
	}

	public void receiveChatRequest() {

		// ENTER YOUR CODE TO RECEIVE A CHAT REQUEST

		String message = "Chat request details";

		// Notify the chat request details to the GUI
		this.observable.onChatInvitationReceived(message);
	}

	public void acceptChatRequest() {

		// ENTER YOUR CODE TO ACCEPT A CHAT REQUEST
	}

	public void refuseChatRequest() {

		// ENTER YOUR CODE TO REFUSE A CHAT REQUEST
	}

	public void sendChatClosure() {

		// ENTER YOUR CODE TO SEND A CHAT CLOSURE

		this.chatReceiver = null;
	}

	public void receiveChatClosure() {

		// ENTER YOUR CODE TO RECEIVE A CHAT REQUEST

		String message = "Chat request details";

		// Notify the chat request details to the GUI
		this.observable.onChatDisconnect(message);
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
		}
	}
}