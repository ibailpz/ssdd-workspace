package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.IOException;
import java.net.ServerSocket;

public class UploadThread extends Thread {

	private static UploadThread instance;
	public static final int port = 8888;

	private UploadThread() {
	}

	public static void startInstance() {
		instance = new UploadThread();
		instance.start();
	}

	public static UploadThread getInstance() {
		return instance;
	}

	@Override
	public void run() {
		super.run();

		int clientCount = 0;

		try (ServerSocket tcpServerSocket = new ServerSocket(port);) {
			System.out.println(" - Waiting for connections '"
					+ tcpServerSocket.getInetAddress().getHostAddress() + ":"
					+ tcpServerSocket.getLocalPort() + "' ...");

			while (clientCount++ < 5) {
				new EchoService(tcpServerSocket.accept());
			}
		} catch (IOException e) {
			System.err.println("# TCPServer IO error:" + e.getMessage());
		}

	}
}
