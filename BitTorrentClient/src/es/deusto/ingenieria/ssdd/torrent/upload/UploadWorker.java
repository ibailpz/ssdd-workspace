package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class UploadWorker extends Thread {
	private DataInputStream in;
	private DataOutputStream out;
	private Socket tcpSocket;

	public UploadWorker(Socket socket) {
		try {
			this.tcpSocket = socket;
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());
			this.start();
		} catch (IOException e) {
			System.err.println("# TCPConnection IO error:" + e.getMessage());
		}
	}

	public void run() {
		// Echo server
		try {
			String data = this.in.readUTF();
			
			
			
			
		} catch (EOFException e) {
			System.err.println("# TCPConnection EOF error" + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPConnection IO error:" + e.getMessage());
		} finally {
			try {
				tcpSocket.close();
			} catch (IOException e) {
				System.err
						.println("# TCPConnection IO error:" + e.getMessage());
			}
		}
		UploadThread.getInstance().childFinished(32);
	}
}