package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import es.deusto.ingenieria.ssdd.torrent.data.BlockTemp;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.main.WindowManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadThread extends Thread {

	private static DownloadThread instance;
	private List<Peer> peerList;
	private Semaphore block;
	// private Semaphore noPeers = new Semaphore(0);
	private HashMap<Integer, BlockTemp> blocksControl = new HashMap<>();
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
		System.out.println("DownloadThread - Peers updated");
		this.peerList = newPeerList;
		// noPeers.release(Integer.MAX_VALUE);
		// noPeers = new Semaphore(0);
	}

	@Override
	public void run() {
		super.run();
		System.out.println("DownloadThread - DownloadThread started");

		// block = new Semaphore(peerList.size());
		block = new Semaphore(30);
		finished = FileManager.getFileManager().isFinished();

		int[] blockSubBlock = new int[3];
		boolean exit = false;
		donwloadedBytes = 0;
		while (!finished && !exit) {
			while (!finished && !exit) {
				System.out
						.println("DownloadThread - Getting new peer to download...");
				Peer peer = getNextPeer(blockSubBlock);
				if (peer != null) {
					new DownloadWorkerThreaded(FileManager.getFileManager()
							.getInfoHash(), peer, blockSubBlock[0],
							blockSubBlock[1], blockSubBlock[2]).start();
					try {
						block.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
						exit = true;
					}
				} else {
					System.out
							.println("No peers available. Waiting for update...");
					// try {
					// noPeers.acquire();
					// } catch (InterruptedException e) {
					// e.printStackTrace();
					// exit = true;
					// }
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						exit = true;
					}
				}
				if (isInterrupted()) {
					exit = true;
				}
			}
			finished = FileManager.getFileManager().checkAndWriteFile();
		}
		System.out.println("DownloadThread - DownloadThread stopped and "
				+ (finished ? "" : "not ") + "finished downloading");
		// TrackerThread.getInstance().interrupt();
		// finished = FileManager.getFileManager().checkAndWriteFile();
		WindowManager.exitBlocker.release();
	}

	private Peer getNextPeer(int[] blockSubBlock) {
		int index = 0;
		int pos = FileManager.getFileManager().getNextPosToDownload(index);
		while (pos >= 0) {
			BlockTemp bt = null;
			synchronized (blocksControl) {
				bt = blocksControl.get(pos);
				if (bt == null) {
					bt = new BlockTemp(pos, FileManager.getFileManager()
							.getBlockSize(pos), TrackerThread.subBlockSize);
					System.out.println("BlockTemp created for position " + pos);
				}
			}
			if (bt.hasMoreMiniBlocks()) {
				for (int i = 0; i < peerList.size(); i++) {
					Peer p = peerList.get(i);
					if (p.isChockedUs()) {
						peerList.remove(i);
						i--;
					} else if (p.getBitfield()[pos] != 0) {
						blockSubBlock[0] = pos;
						blockSubBlock[1] = bt.getNextMiniBlock();
						blockSubBlock[2] = bt
								.getMiniBlockSize(blockSubBlock[1]);
						blocksControl.put(pos, bt);
						peerList.add(peerList.remove(i));
						return p;
					}
				}
			}
			index++;
			pos = FileManager.getFileManager().getNextPosToDownload(index);
		}
		return null;
	}

	void childFinished(int blockPos, int offset, byte[] bytes) {
		System.out.println("DownloadThread - Child for block " + blockPos
				+ " and offset " + offset + " finished");
		// BlockTemp bt = new BlockTemp(blockPos, 0, 0);
		// int index = blocksControl.indexOf(bt);
		// // if (index >= 0) {
		// bt = blocksControl.get(index);
		// // } else {
		// // blocksControl.add(bt);
		// // }
		BlockTemp bt = blocksControl.get(blockPos);
		if (bytes != null) {
			System.out
					.println("DownloadThread - Adding child result for block "
							+ blockPos + " and offset " + offset);
			donwloadedBytes += bytes.length;

			bt.addBytes(bytes, offset);
			if (bt.isFinished()) {
				System.out.println("DownloadThread - Block " + blockPos
						+ " finished");
				try {
					finished = FileManager.getFileManager().checkAndSaveBlock(
							bt.getPos(), bt.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
				blocksControl.remove(blockPos);
			}
		} else {
			System.out.println("DownloadThread - Child for block " + blockPos
					+ " and offset " + offset + " did not get any data");
			bt.miniBlockDownloadFailed(offset);
		}
		block.release();
	}

}
