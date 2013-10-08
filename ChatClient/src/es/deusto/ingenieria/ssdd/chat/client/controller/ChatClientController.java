package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import es.deusto.ingenieria.ssdd.util.observer.local.LocalObservable;
import es.deusto.ingenieria.ssdd.chat.data.User;

public class ChatClientController {
	private String serverIP;
	private int serverPort;
	private User connectedUser;
	private User chatReceiver;
	private LocalObservable observable;
	
	public ChatClientController() {
		this.observable = new LocalObservable();
		this.serverIP = null;
		this.serverPort = -1;
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
	
	public void addLocalObserver(Observer observer) {
		this.observable.addObserver(observer);
	}
	
	public void deleteLocalObserver(Observer observer) {
		this.observable.deleteObserver(observer);
	}
	
	public boolean connect(String ip, int port, String nick) {
		
		//ENTER YOUR CODE TO CONNECT
		
		this.connectedUser = new User();
		this.connectedUser.setNick(nick);
		this.serverIP = ip;
		this.serverPort = port;
		
		return true;
	}
	
	public boolean disconnect() {
		
		//ENTER YOUR CODE TO DISCONNECT
		
		this.connectedUser = null;
		this.chatReceiver = null;
		
		return true;
	}
	
	public List<String> getConnectedUsers() {
		List<String> connectedUsers = new ArrayList<>();
		
		//ENTER YOUR CODE TO OBTAIN THE LIST OF CONNECTED USERS
		connectedUsers.add("Default");
		
		return connectedUsers;
	}
	
	public boolean sendMessage(String message) {
		
		//ENTER YOUR CODE TO SEND A MESSAGE
		
		return true;
	}
	
	public void receiveMessage() {
		
		//ENTER YOUR CODE TO RECEIVE A MESSAGE
		
		String message = "Received message";		
		
		//Notify the received message to the GUI
		this.observable.notifyObservers(message);
	}	
	
	public boolean sendChatRequest(String to) {
		
		//ENTER YOUR CODE TO SEND A CHAT REQUEST
		
		this.chatReceiver = new User();
		this.chatReceiver.setNick(to);
		
		return true;
	}	
	
	public void receiveChatRequest() {
		
		//ENTER YOUR CODE TO RECEIVE A CHAT REQUEST
		
		String message = "Chat request details";
		
		//Notify the chat request details to the GUI
		this.observable.notifyObservers(message);
	}
	
	public boolean acceptChatRequest() {
		
		//ENTER YOUR CODE TO ACCEPT A CHAT REQUEST
		
		return true;
	}
	
	public boolean refuseChatRequest() {
		
		//ENTER YOUR CODE TO REFUSE A CHAT REQUEST
		
		return true;
	}	
	
	public boolean sendChatClosure() {
		
		//ENTER YOUR CODE TO SEND A CHAT CLOSURE
		
		this.chatReceiver = null;
		
		return true;
	}
	
	public void receiveChatClosure() {
		
		//ENTER YOUR CODE TO RECEIVE A CHAT REQUEST
		
		String message = "Chat request details";
		
		//Notify the chat request details to the GUI
		this.observable.notifyObservers(message);
	}
}