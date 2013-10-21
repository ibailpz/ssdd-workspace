package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {

	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;
	private Thread process;
	private static final String groupIP = "228.5.6.7";
	private static final int port = 6789;

	private void processRequest(DatagramPacket request) {
		String message = new String(request.getData()).trim();
		String[] split = message.split(" ");

		if (!isConnected()) {
			return;
		}

		if (split[0].equals("connect")) {
			if (split[1].equals(connectedUser.getNick())) {
				try {
					sendMessage("error_nick " + connectedUser.getNick());
					setDisconnected();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {

				if (isConnected()) {

					try {
						sendMessage("wellcome_my_nick "
								+ connectedUser.getNick());
						observable.onConnect(true);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else{
					this.connectedUser = new User();
					this.connectedUser.setNick(split[1]);
				}
			}
		} else if (split[0].equals("wellcome_my_nick")) {
			observable.onUserConnected(split[1]);
		} else if (split[0].equals("disconnect")) {
			observable.onUserDisconnected(split[1]);
		} else if (!split[2].equals(connectedUser.getNick())) {
			return;
		} else if (split[0].equals("error_nick")) {
			connectedUser = null;
			this.observable.onError("ERROR nick");
		} else {
			this.observable.onConnect(true);
			if (split[0].equals("message")) {
				message(message.substring(split[0].length() + split[1].length()
						+ 2));
			} else if (split[0].equals("invitation")) {
				onChatRequest(split[1]);
			} else if (split[0].equals("close_chat")) {
				chatClosure(split[1]);
			} else if (split[0].equals("accept")) {
				this.chatReceiver = new User();
				this.chatReceiver.setNick(split[1]);
				this.observable.onChatRequestResponse(split[1], split[2], true);
			} else if (split[0].equals("busy")) {
				this.observable
						.onChatRequestResponse(split[1], split[2], false);
			} else if (split[0].equals("cancel_invitation")) {
				this.observable.onInvitationCancelled(split[1], split[2]);
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

	public void connect(final String groupIP, final int port, final String nick)
			throws IOException {

		process = new Thread() {

			@Override
			public void run() {
				for (; !this.isInterrupted();) {
					try (MulticastSocket socket = new MulticastSocket(
							ChatClientController.port)) {
						InetAddress group = InetAddress
								.getByName(ChatClientController.groupIP);
						socket.joinGroup(group);

						byte[] buffer = new byte[1024];
						DatagramPacket messageIn = new DatagramPacket(buffer,
								buffer.length);

						socket.receive(messageIn);

						System.out.println(" - Received a message from '"
								+ messageIn.getAddress().getHostAddress() + ":"
								+ messageIn.getPort() + "' -> "
								+ new String(messageIn.getData()));

						processRequest(messageIn);
						socket.leaveGroup(group);
					} catch (SocketException e) {
						System.err.println("# Socket Error: " + e.getMessage());
					} catch (IOException e) {
						System.err.println("# IO Error: " + e.getMessage());
					}
				}
			}
		};

		process.start();
		sendCommand("connect " + nick);
	}

	public void disconnect() {
		try {
			sendCommand("disconnect " + connectedUser.getNick());
		} catch (IOException e) {
			e.printStackTrace();
		}

		setDisconnected();
	}

	public void setDisconnected() {
		this.connectedUser = null;
		this.chatReceiver = null;
		if (process != null) {
			process.interrupt();
		}
	}

	public void sendMessage(String message) throws IOException {
		sendCommand("message " + connectedUser.getNick() + " "
				+ chatReceiver.getNick() + " " + message);
	}

	public void message(String message) {
		// Notify the received message to the GUI
		this.observable.onMessage(message, connectedUser.getNick(),
				chatReceiver.getNick());
	}

	public void sendChatRequest(String to) throws IOException {
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		sendCommand("invitation " + connectedUser.getNick() + " " + to);
	}

	public void onChatRequest(String userFrom) {
		// Notify the chat request details to the GUI
		this.observable.onChatInvitation(userFrom);
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

	public void chatClosure(String userFrom) {
		// Notify the chat request details to the GUI
		this.observable.onChatDisconnect(chatReceiver.getNick());
		chatReceiver = null;
	}

	private void sendCommand(String command) throws IOException {
		try (MulticastSocket socket = new MulticastSocket(0)) {
			InetAddress group = InetAddress.getByName(groupIP);
			socket.joinGroup(group);

			DatagramPacket messageOut = new DatagramPacket(command.getBytes(),
					command.length(), group, port);
			socket.send(messageOut);

			System.out.println(" - Sent a message to '"
					+ messageOut.getAddress().getHostAddress() + ":"
					+ messageOut.getPort() + "' -> "
					+ new String(messageOut.getData()));

			socket.leaveGroup(group);
		} catch (SocketException e) {
			System.err.println("# Socket Error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# IO Error: " + e.getMessage());
		}
	}
}