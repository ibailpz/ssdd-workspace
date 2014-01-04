package es.deusto.ingenieria.ssdd.torrent.download;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;

public interface InputListener {
	
	public void handshakeReceived();
	
	public void messageReceived(PeerProtocolMessage message);

}
