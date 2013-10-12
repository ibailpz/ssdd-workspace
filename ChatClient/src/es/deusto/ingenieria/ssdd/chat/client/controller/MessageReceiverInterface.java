package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.util.List;

public interface MessageReceiverInterface {

	public void onMessageReceived(String message, String user);

	public void onConnect(boolean connected);

	public void onUsersUpdated(List<String> users);

	public void onError(String error);

	public void onChatRequestResponse(String user, boolean accept);

	public void onChatDisconnect(String user);
	
	public void onChatInvitationReceived(String user);

}
