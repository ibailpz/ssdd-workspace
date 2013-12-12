package es.deusto.ingenieria.ssdd.torrent.download;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import es.deusto.ingenieria.ssdd.torrent.data.BlockTemp;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;

public class DownloadThread extends Thread {

	private static DownloadThread instance;
	private List<Peer> peerList;
	private Semaphore block;
	private ArrayList<BlockTemp> blocksControl = new ArrayList<>();
	

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
		
		block = new Semaphore(peerList.size());

		for (;;) {
			new DownloadWorker(null, 0, 0, 0);
			try {
				block.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
	void childFinished(int numBytes) {
		block.release();
	}

}
