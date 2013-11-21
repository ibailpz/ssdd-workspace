package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.util.ArrayList;

import javax.jms.InvalidClientIDException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;

import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {

	private User connectedUser;
	private User chatReceiver;
	private MessageReceiverInterface observable;

	private String connectionFactoryName = "TopicConnectionFactory";
	private String topicJNDIName = "ssdd.topic";
	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private TopicPublisher topicPublisher = null;
	private TopicSubscriber topicSubscriber = null;

	private ArrayList<String> myMessages = new ArrayList<>();

	private String connectionQueueFactoryName = "QueueConnectionFactory";
	private String queueJNDIName = "dynamicQueues/ssdd.queue.";
	private QueueConnection queueConnection = null;
	private QueueSession queueSession = null;
	private QueueSender queueSender = null;
	private QueueReceiver queueReceiver = null;

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
		try {
			// JNDI Initial Context
			Context ctx = new InitialContext();
			// Connection Factory
			TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx
					.lookup(connectionFactoryName);
			// Message Destination
			Topic myTopic = (Topic) ctx.lookup(topicJNDIName);

			// Connections
			topicConnection = topicConnectionFactory.createTopicConnection();
			topicConnection.setClientID(nick);
			System.out.println("- Topic Connection created!");
			// Sessions
			topicSession = topicConnection.createTopicSession(false,
					Session.AUTO_ACKNOWLEDGE);
			System.out.println("- Topic Session created!");
			// Topic Listener
			topicSubscriber = topicSession.createSubscriber(myTopic,
					"everyone_filter = true OR target_user = '" + nick + "'",
					false);
			topicSubscriber.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					System.out.println(message);
					observable.onConnect(true);
					try {
						String text = ((TextMessage) message).getText();
						if (myMessages.contains(text)) {
							myMessages.remove(text);
							if (text.startsWith("disconnect")) {
								setDisconnected();
							}
							return;
						}
						String split[] = text.split(" ");
						if (split[0].equals("connect")) {
							try {
								sendCommandTopic("welcome_my_nick "
										+ connectedUser.getNick(), false,
										split[1]);
								observable.onUserConnected(split[1]);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if (split[0].equals("welcome_my_nick")) {
							observable.onUserConnected(split[1]);
						} else if (split[0].equals("disconnect")) {
							observable.onUserDisconnected(split[1]);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			// Begin message delivery
			topicConnection.start();

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

			connectedUser = new User();
			connectedUser.setNick(nick);
			createQueue();
		} catch (InvalidClientIDException e) {
			// e.printStackTrace();
			this.observable.onError("ERROR nick");
		}
	}

	public void disconnect() {
		try {
			sendCommandTopic("disconnect " + connectedUser.getNick(), true,
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDisconnected() {
		myMessages.clear();
		this.connectedUser = null;
		this.chatReceiver = null;
		try {
			topicPublisher.close();
			topicSubscriber.close();
			topicSession.close();
			topicConnection.close();
			queueReceiver.close();
			queueSession.close();
			queueConnection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		topicPublisher = null;
		topicSubscriber = null;
		topicSession = null;
		topicConnection = null;
		queueReceiver = null;
		queueSession = null;
		queueConnection = null;
	}

	public void sendMessage(String message) throws Exception {
		sendCommandQueue("message " + connectedUser.getNick() + " "
				+ chatReceiver.getNick() + " " + message);
	}

	public void message(String message) {
		// Notify the received message to the GUI
		this.observable.onMessage(message, connectedUser.getNick(),
				chatReceiver.getNick());
	}

	public void sendChatRequest(String to) throws Exception {
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		createQueueFor(to);
		sendCommandQueue("invitation " + connectedUser.getNick() + " " + to);
	}

	public void onChatRequest(String userFrom) throws Exception {
		// Notify the chat request details to the GUI
		this.observable.onChatInvitation(userFrom);
	}

	public void cancelInvitation(String otherUser) throws Exception {
		sendCommandQueue("cancel_invitation " + connectedUser.getNick() + " "
				+ otherUser);
		closeQueue();
	}

	public void acceptChatRequest(String user) throws Exception {
		if (this.chatReceiver != null) {
			sendChatClosure();
		}
		createQueueFor(user);
		this.chatReceiver = new User();
		this.chatReceiver.setNick(user);
		sendCommandQueue("accept " + connectedUser.getNick() + " " + user);
	}

	public void refuseChatRequest(String user) throws Exception {
		if (isChatSessionOpened()) {
			closeQueue();
		}
		createQueueFor(user);
		sendCommandQueue("busy " + connectedUser.getNick() + " " + user);
		closeQueue();
		if (isChatSessionOpened()) {
			createQueueFor(chatReceiver.getNick());
		}
	}

	public void sendChatClosure() throws Exception {
		sendCommandQueue("close_chat " + connectedUser.getNick() + " "
				+ chatReceiver.getNick());
		this.chatReceiver = null;
		closeQueue();
	}

	public void chatClosure(String userFrom) throws Exception {
		// Notify the chat request details to the GUI
		this.observable.onChatDisconnect(userFrom);
		chatReceiver = null;
		closeQueue();
	}

	private void sendCommandTopic(String command, boolean all, String nickTo)
			throws Exception {
		// Text Message
		TextMessage textMessage = topicSession.createTextMessage();
		// Message Properties
		if (all) {
			textMessage.setBooleanProperty("everyone_filter", true);
		} else {
			textMessage.setBooleanProperty("everyone_filter", false);
			textMessage.setStringProperty("target_user", nickTo);
		}
		// Message Body
		textMessage.setText(command);

		myMessages.add(command);

		// Publish the Messages
		topicPublisher.publish(textMessage);
		System.out.println("- TextMessage published in the Topic!");

	}

	private void createQueue() {
		try {
			// JNDI Initial Context
			Context ctx = new InitialContext();

			// Connection Factory
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx
					.lookup(connectionQueueFactoryName);

			// Message Destination
			Queue myQueue = (Queue) ctx.lookup(queueJNDIName
					+ connectedUser.getNick());

			// Connection
			queueConnection = queueConnectionFactory.createQueueConnection();
			System.out.println("- Queue Connection created!");

			// Session
			queueSession = queueConnection.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);
			System.out.println("- Queue Session created!");

			// Message Receiver
			queueReceiver = queueSession.createReceiver(myQueue);
			System.out.println("- QueueReceiver created!");

			queueReceiver.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message mes) {
					try {
						String message = ((TextMessage) mes).getText();
						String split[] = message.split(" ");
						observable.onConnect(true);
						if (split[0].equals("message")) {
							message(message.substring(split[0].length()
									+ split[1].length() + split[2].length() + 3));
						} else if (split[0].equals("invitation")) {
							onChatRequest(split[1]);
						} else if (split[0].equals("close_chat")) {
							chatClosure(split[1]);
						} else if (split[0].equals("accept")) {
							chatReceiver = new User();
							chatReceiver.setNick(split[1]);
							observable.onChatRequestResponse(split[1],
									split[2], true);

						} else if (split[0].equals("busy")) {
							observable.onChatRequestResponse(split[1],
									split[2], false);

						} else if (split[0].equals("cancel_invitation")) {
							observable
									.onInvitationCancelled(split[1], split[2]);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			// Start receiving messages
			queueConnection.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createQueueFor(String nickTo) throws Exception {
		// JNDI Initial Context
		Context ctx = new InitialContext();
		// Connection Factory
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx
				.lookup(connectionFactoryName);
		// Message Destination
		Queue myQueue = (Queue) ctx.lookup(queueJNDIName + nickTo);
		// Connection
		queueConnection = queueConnectionFactory.createQueueConnection();
		System.out.println("- Queue Connection created!");
		// Session
		queueSession = queueConnection.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		System.out.println("- Queue Session created!");
		// Message Producer
		queueSender = queueSession.createSender(myQueue);
		System.out.println("- QueueSender created!");
	}

	private void sendCommandQueue(String command) throws Exception {
		// Text Message
		TextMessage textMessage = queueSession.createTextMessage();
		// Message Body
		textMessage.setText(command);
		// Send the Messages
		queueSender.send(textMessage);
	}

	private void closeQueue() throws Exception {
		queueSender.close();
		queueSender = null;
	}
}