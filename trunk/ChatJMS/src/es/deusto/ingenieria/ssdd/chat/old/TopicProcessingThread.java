package es.deusto.ingenieria.ssdd.chat.old;

import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;

import es.deusto.ingenieria.ssdd.chat.client.controller.ChatClientController;


public class TopicProcessingThread extends Thread {

	private ChatClientController controller;
	private boolean stop = false;
	private String connectionFactoryName = "TopicConnectionFactory";
	private String topicJNDIName = "ssdd.topic";
	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private TopicSubscriber topicSubscriber = null;
	private String nick;

	public TopicProcessingThread(ChatClientController c, String nick) {
		controller = c;
		this.nick = nick;
	}

	@Override
	public void run() {
		// try {
		// Thread.sleep(5000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// if (!controller.isErrorNickReceived()) {
		// controller.setDelayPassed();
		// } else {
		// return;
		// }
		for (; !stop;) {
			try {
				Context ctx = new InitialContext();

				// Connection Factories
				TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx
						.lookup(connectionFactoryName);
				// Message Destinations
				Topic myTopic = (Topic) ctx.lookup(topicJNDIName);
				// Connections
				topicConnection = topicConnectionFactory
						.createTopicConnection();
				topicConnection.setClientID("SSDD_TopicSubscriber");
				System.out.println("- Topic Connection created!");
				// Sessions
				topicSession = topicConnection.createTopicSession(false,
						Session.AUTO_ACKNOWLEDGE);
				System.out.println("- Topic Session created!");
				// Topic Listener
				topicSubscriber = topicSession.createDurableSubscriber(myTopic,
						nick, "Filter = '1'", false);
				TopicListener topicListener = new TopicListener();
				topicSubscriber.setMessageListener(topicListener);
				// Begin message delivery
				topicConnection.start();

				// Wait for messages
				System.out.println("- Waiting 10 seconds for messages...");
				Thread.sleep(10000);
			} catch (Exception e) {
				System.err.println("# TopicSubscriberTest Error: "
						+ e.getMessage());
			}
		}
		try {
			// Close resources
			topicSubscriber.close();
			topicSession.unsubscribe(nick);
			topicSession.close();
			topicConnection.close();
			System.out.println("- Topic resources closed!");
		} catch (Exception ex) {
			System.err.println("# TopicSubscriberTest Error: "
					+ ex.getMessage());
		}
	}

	@Override
	public void interrupt() {
		stop = true;
	}

}
