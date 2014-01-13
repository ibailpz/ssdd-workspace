package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Semaphore;

import es.deusto.ingenieria.ssdd.torrent.main.WindowManager;

public class UploadThread extends Thread {

	private static UploadThread instance;
	public static final int port = 8888;
	private ServerSocket tcpServerSocket;
	private Semaphore block = new Semaphore(10);
	private long totalBytes = 0;

	private UploadThread() {
		super("UploadThread");
		this.setDaemon(false);
	}

	public static void startInstance() {
		instance = new UploadThread();
		instance.start();
	}

	public static UploadThread getInstance() {
		return instance;
	}

	public long getTotalBytes() {
		return totalBytes;
	}

	@Override
	public void run() {
		super.run();

		System.out.println("UploadThread - UploadThread started");

		try (ServerSocket tcpServerSocket = new ServerSocket(port);) {
			this.tcpServerSocket = tcpServerSocket;
			System.out.println("UploadThread - Waiting for connections...");
			for (;;) {
				new UploadWorker(tcpServerSocket.accept()).start();
				try {
					block.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
				if (isInterrupted()) {
					break;
				}
			}
		} catch (IOException e) {
			System.err.println("# TCPServer IO error:" + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("UploadThread - UploadThread stopped");
		WindowManager.exitBlocker.release();
	}

	@Override
	public void interrupt() {
		try {
			tcpServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.interrupt();
	}

	void childFinished(int numBytes) {
		block.release();
		totalBytes += numBytes;
	}
}
