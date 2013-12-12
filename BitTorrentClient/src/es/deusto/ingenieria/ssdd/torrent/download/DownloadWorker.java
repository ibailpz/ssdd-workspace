package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class DownloadWorker extends Thread {

	private String peerIP;
	private int port;
	private int block;
	private int subBlock;

	// TODO Completar parametros
	public DownloadWorker(String peerIP, int port, int block, int subBlock) {
		this.peerIP = peerIP;
		this.port = port;
		this.block = block;
		this.subBlock = subBlock;
	}

	@Override
	public void run() {
		super.run();

		try (Socket tcpSocket = new Socket(peerIP, port);
				DataInputStream in = new DataInputStream(
						tcpSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(
						tcpSocket.getOutputStream())) {
			
			System.out.println(" - Sent data to '"
					+ tcpSocket.getInetAddress().getHostAddress() + ":"
					+ tcpSocket.getPort() + "' -> '");
			
			
			
			

		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
		}
	}

}
