package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Semaphore;

public class UploadThread extends Thread {

	private static UploadThread instance;
	public static final int port = 8888;
	private Semaphore block = new Semaphore(5);
	private long totalBytes = 0;

	private UploadThread() {
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

		try (ServerSocket tcpServerSocket = new ServerSocket(port);) {
			System.out.println(" - Waiting for connections '"
					+ tcpServerSocket.getInetAddress().getHostAddress() + ":"
					+ tcpServerSocket.getLocalPort() + "' ...");

			for(;;) {
				new UploadWorker(tcpServerSocket.accept());
				try {
					block.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		} catch (IOException e) {
			System.err.println("# TCPServer IO error:" + e.getMessage());
		}

	}
	
	void childFinished(int numBytes) {
		block.release();
		totalBytes += numBytes;
	}
}
