package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {

	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;
	private ProcessingThread process;
	private static final String groupIP = "228.5.6.7";
	private static final int port = 6789;
	private List<String> sentMessages = new ArrayList<>();
	private boolean sentNickError = false;
	private boolean startingChat = false;

	public void processRequest(DatagramPacket request) {
		String message = new String(request.getData()).trim();
		String[] split = message.split(" ");

		if (sentMessages.contains(message)) {
			sentMessages.remove(message);
			if (split[0].equals("disconnect")) {
				setDisconnected();
			}
			System.out.println("     - '" + message + "' Own message, ignored");
			return;
		}

		// Ignore every message if the user is not connected
		if (!isConnected()) {
			System.out.println("     - Not connected, message ignored");
			return;
		}

		// Check if the message is global
		if (split.length <= 2) {
			if (split[0].equals("connect")) {
				if (split[1].equals(connectedUser.getNick())) {
					try {
						sentNickError = true;
						sendCommand("error_nick " + connectedUser.getNick());
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						observable.onConnect(true);
						sendCommand("welcome_my_nick "
								+ connectedUser.getNick());
						observable.onUserConnected(split[1]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else if (split[0].equals("welcome_my_nick")) {
				if (!split[1].equals(connectedUser.getNick())) {
					observable.onConnect(true);
					observable.onUserConnected(split[1]);
				}
			} else if (split[0].equals("disconnect")) {
				if (!split[1].equals(connectedUser.getNick())) {
					observable.onUserDisconnected(split[1]);
				}
			} else if (split[0].equals("error_nick")) {
				if (split[1].equals(connectedUser.getNick()) && !sentNickError) {
					setDisconnected();
					this.observable.onError("ERROR nick");
				}
			}
		} else { // or the message is particular for the user
			if (!split[2].equals(connectedUser.getNick())) {
				// If the message is particular but not for the user, ignore it
				return;
			} else {
				this.observable.onConnect(true);
				if (split[0].equals("message")) {
					message(message.substring(split[0].length()
							+ split[1].length() + split[2].length() + 3));
				} else if (split[0].equals("invitation")) {
					onChatRequest(split[1]);
				} else if (split[0].equals("close_chat")) {
					chatClosure(split[1]);
				} else if (split[0].equals("accept")) {
					if (startingChat) {
						this.chatReceiver = new User();
						this.chatReceiver.setNick(split[1]);
						this.observable.onChatRequestResponse(split[1],
								split[2], true);
						startingChat = false;
					} else {
						try {
							this.chatReceiver = new User();
							this.chatReceiver.setNick(split[1]);
							sendChatClosure();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else if (split[0].equals("busy")) {
					if (startingChat) {
						this.observable.onChatRequestResponse(split[1],
								split[2], false);
						startingChat = false;
					}
				} else if (split[0].equals("cancel_invitation")) {
					System.out.println("cancel_invitation");
					if (startingChat) {
						this.observable.onInvitationCancelled(split[1],
								split[2]);
						startingChat = false;
					}
				}
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

	public void connect(final String nick)
			throws IOException {
		this.connectedUser = new User();
		this.connectedUser.setNick(nick);

		process = new ProcessingThread(ChatClientController.groupIP,
				ChatClientController.port, this);

		process.start();
		sendCommand("connect " + nick);
	}

	public void disconnect() {
		try {
			sendCommand("disconnect " + connectedUser.getNick());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setDisconnected() {
		if (process != null) {
			process.interrupt();
			process = null;
		}
		this.connectedUser = null;
		this.chatReceiver = null;
		sentMessages.clear();
		sentNickError = false;
		startingChat = false;
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
		startingChat = true;
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		sendCommand("invitation " + connectedUser.getNick() + " " + to);
	}

	public void onChatRequest(String userFrom) {
		startingChat = true;
		// Notify the chat request details to the GUI
		this.observable.onChatInvitation(userFrom);
	}

	public void cancelInvitation(String otherUser) throws IOException {
		startingChat = false;
		sendCommand("cancel_invitation " + connectedUser.getNick() + " "
				+ otherUser);
	}

	public void acceptChatRequest(String user) throws IOException {
		startingChat = false;
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		this.chatReceiver = new User();
		this.chatReceiver.setNick(user);
		sendCommand("accept " + connectedUser.getNick() + " " + user);
	}

	public void refuseChatRequest(String user) throws IOException {
		startingChat = false;
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

	private void sendCommand(final String command) throws IOException {
		try (MulticastSocket socket = new MulticastSocket(0)) {
			InetAddress group = InetAddress.getByName(groupIP);
			socket.joinGroup(group);

			DatagramPacket messageOut = new DatagramPacket(command.getBytes(),
					command.length(), group, port);
			sentMessages.add(command);
			socket.send(messageOut);

			System.out.println(" - Sent a message to '"
					+ messageOut.getAddress().getHostAddress() + ":"
					+ messageOut.getPort() + "' -> "
					+ new String(messageOut.getData()));

			socket.leaveGroup(group);
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					if (sentMessages.contains(command) && isConnected()) {
						try {
							sendCommand(command);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}, 1000);
		} catch (SocketException e) {
			System.err.println("# Socket Error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# IO Error: " + e.getMessage());
		}
	}
}