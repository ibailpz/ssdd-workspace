package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {

	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;
	private TopicProcessingThread process;
	private String connectionFactoryName = "TopicConnectionFactory";
	private String topicJNDIName = "ssdd.topic";
	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private TopicPublisher topicPublisher = null;
	private boolean errorNickReceived = false;
	private boolean delayPassed = false;

	public void processRequest(DatagramPacket request) {
		String message = new String(request.getData()).trim();
		String[] split = message.split(" ");

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
				if (split[1].equals(connectedUser.getNick())) {
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
					this.chatReceiver = new User();
					this.chatReceiver.setNick(split[1]);
					this.observable.onChatRequestResponse(split[1], split[2],
							true);

				} else if (split[0].equals("busy")) {
					this.observable.onChatRequestResponse(split[1], split[2],
							false);

				} else if (split[0].equals("cancel_invitation")) {
					System.out.println("cancel_invitation");
					this.observable.onInvitationCancelled(split[1], split[2]);

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

	public void connect(final String nick) throws Exception {
		this.connectedUser = new User();
		this.connectedUser.setNick(nick);

		process = new TopicProcessingThread(this, nick);
		process.start();
		
		// JNDI Initial Context
		Context ctx = new InitialContext();
		// Connection Factory
		TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx
				.lookup(connectionFactoryName);
		// Message Destination
		Topic myTopic = (Topic) ctx.lookup(topicJNDIName);
		// Connection
		topicConnection = topicConnectionFactory.createTopicConnection();
		System.out.println("- Topic Connection created!");
		// Session
		topicSession = topicConnection.createTopicSession(false,
				Session.AUTO_ACKNOWLEDGE);
		System.out.println("- Topic Session created!");
		// Message Publisher
		topicPublisher = topicSession.createPublisher(myTopic);
		System.out.println("- TopicPublisher created!");

		sendCommandTopic("connect " + nick, true, null);
		
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
		if (process != null) {
			process.interrupt();
			process = null;
		}
		// Close resources
		try {
			topicPublisher.close();
			topicSession.close();
			topicConnection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		topicPublisher = null;
		topicSession = null;
		topicConnection = null;
		this.connectedUser = null;
		this.chatReceiver = null;
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

	private void sendCommand(final String command) throws IOException {
		/*
		 * try (MulticastSocket socket = new MulticastSocket(0)) { InetAddress
		 * group = InetAddress.getByName(groupIP); socket.joinGroup(group);
		 * 
		 * DatagramPacket messageOut = new DatagramPacket(command.getBytes(),
		 * command.length(), group, port); sentMessages.add(command);
		 * socket.send(messageOut);
		 * 
		 * System.out.println(" - Sent a message to '" +
		 * messageOut.getAddress().getHostAddress() + ":" + messageOut.getPort()
		 * + "' -> " + new String(messageOut.getData()));
		 * 
		 * socket.leaveGroup(group); new Timer().schedule(new TimerTask() {
		 * 
		 * @Override public void run() { if (sentMessages.contains(command) &&
		 * isConnected()) { try { sendCommand(command); } catch (IOException e)
		 * { e.printStackTrace(); } } } }, 1000); } catch (SocketException e) {
		 * System.err.println("# Socket Error: " + e.getMessage()); } catch
		 * (IOException e) { System.err.println("# IO Error: " +
		 * e.getMessage()); }
		 */
	}

	private void sendCommandTopic(String command, boolean all, String nick)
			throws Exception {
		// Text Message
		TextMessage textMessage = topicSession.createTextMessage();
		// Message Properties
		if (all) {
			textMessage.setBooleanProperty("everyone_filter", true);
		} else {
			textMessage.setBooleanProperty("everyone_filter", false);
			textMessage.setStringProperty("target_user", nick);
		}
		// Message Body
		textMessage.setText(command);

		// Publish the Messages
		topicPublisher.publish(textMessage);
		System.out.println("- TextMessage published in the Topic!");

	}

	public boolean isErrorNickReceived() {
		return errorNickReceived;
	}

	public void setErrorNickReceived() {
		this.errorNickReceived = true;
	}

	public boolean isDelayPassed() {
		return delayPassed;
	}

	public void setDelayPassed() {
		this.delayPassed = true;
	}
}