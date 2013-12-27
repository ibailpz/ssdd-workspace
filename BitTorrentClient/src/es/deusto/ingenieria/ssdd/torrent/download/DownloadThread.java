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

		int[] blockSubBlock = new int[2];
		while (!finished) {
			donwloadedBytes = 0;
			while (!finished) {
				Peer peer = getNextPeer(blockSubBlock);
				if (peer != null) {
					new DownloadWorker(FileManager.getFileManager()
							.getInfoHash(), peer, blockSubBlock[0], blockSubBlock[1])
							.start();
					try {
						block.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (isInterrupted()) {
					return;
				}
			}
			finished = FileManager.getFileManager().checkAndWriteFile();
		}
		TrackerThread.getInstance().interrupt();
	}

	private Peer getNextPeer(int[] blockSubBlock) {
		Peer peer = null;
		for (int i = 0; i < peerList.size(); i++) {
			Peer p = peerList.get(i);
			if (p.isChockedUs()) {
				peerList.remove(i);
				i--;
			} else {
				for (BlockTemp bt : blocksControl) {
					if (p.getBitfield()[bt.getPos()] != 0) {
						blockSubBlock[0] = bt.getPos();
						blockSubBlock[1] = bt.getStartOffset();
						return p;
					}
				}
			}
		}
		if (peer == null) {
			int index = 0;
			int pos = FileManager.getFileManager().getNextPosToDownload(index);
			while (pos >= 0 && peer == null) {
				for (Peer p : peerList) {
					if (p.getBitfield()[pos] != 0) {
						blockSubBlock[0] = pos;
						blockSubBlock[1] = 0;
						return p;
					}
				}
				pos = FileManager.getFileManager().getNextPosToDownload(index);
			}
		}
		return peer;
	}

	void childFinished(int blockPos, int offset, byte[] bytes) {
		if (bytes != null) {
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
		}
		block.release();
	}

}
