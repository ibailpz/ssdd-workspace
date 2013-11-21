package es.deusto.ingenieria.ssdd.chat.client.controller;

public interface MessageReceiverInterface {

	public void onMessage(String message, String userFrom, String userTo);
	
	public void onUserConnected(String user);

	public void onUserDisconnected(String user);

	public void onError(String error);

	public void onChatRequestResponse(String userFrom, String userTo,
			boolean accept);

	public void onChatDisconnect(String userFrom);

	public void onChatInvitation(String userFrom);

	public void onInvitationCancelled(String userFrom, String userTo);

}
