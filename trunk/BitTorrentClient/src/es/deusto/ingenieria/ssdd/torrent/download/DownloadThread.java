package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import es.deusto.ingenieria.ssdd.torrent.data.BlockTemp;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadThread extends Thread {

	private static DownloadThread instance;
	private List<Peer> peerList;
	private Semaphore block;
	private ArrayList<BlockTemp> blocksControl = new ArrayList<>();
	private long donwloadedBytes = 0;
	private boolean finished;

	private DownloadThread(List<Peer> peerList) {
		super("DownloadThread");
		this.setDaemon(false);
		this.peerList = peerList;
	}

	public static void startInstance(List<Peer> peerList) {
		instance = new DownloadThread(peerList);
		instance.start();
	}

	public static DownloadThread getInstance() {
		return instance;
	}

	public long getDownloadedBytes() {
		return donwloadedBytes;
	}

	public void updatePeers(List<Peer> newPeerList) {
		// TODO
		this.peerList = newPeerList;
	}

	@Override
	public void run() {
		super.run();

		block = new Semaphore(peerList.size());
		finished = FileManager.getFileManager().isFinished();

		while (!finished) {
			donwloadedBytes = 0;
			while (!finished) {
				// TODO Set parameters
				new DownloadWorker(null, null, 0, 0);
				try {
					block.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (isInterrupted()) {
					return;
				}
			}
			finished = FileManager.getFileManager().checkAndWriteFile();
		}
		TrackerThread.getInstance().interrupt();
	}

	void childFinished(int blockPos, int offset, byte[] bytes) {
		donwloadedBytes += bytes.length;
		BlockTemp bt = new BlockTemp(blockPos, FileManager.getFileManager()
				.getBlockLength());
		int index = blocksControl.indexOf(bt);
		if (index >= 0) {
			bt = blocksControl.get(index);
		} else {
			blocksControl.add(bt);
		}

		bt.addBytes(bytes, offset);
		if (bt.isFinished()) {
			try {
				finished = FileManager.getFileManager().checkAndSaveBlock(
						bt.getPos(), bt.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		block.release();
	}

}
