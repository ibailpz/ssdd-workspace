package es.deusto.ingenieria.ssdd.torrent.download;

import java.util.List;

import es.deusto.ingenieria.ssdd.torrent.data.Peer;

public class DownloadThread extends Thread {

	private static DownloadThread instance;
	private List<Peer> peerList;

	private DownloadThread(List<Peer> peerList) {
		this.peerList = peerList;
	}

	public static void startInstance(List<Peer> peerList) {
		instance = new DownloadThread(peerList);
		instance.start();
	}
	
	public static DownloadThread getInstance() {
		return instance;
	}

	public void updatePeers(List<Peer> newPeerList) {
		// TODO
		this.peerList = newPeerList;
	}

	@Override
	public void run() {
		super.run();

		
	}

}
