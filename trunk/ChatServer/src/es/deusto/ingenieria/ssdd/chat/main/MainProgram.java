package es.deusto.ingenieria.ssdd.chat.main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import es.deusto.ingenieria.ssdd.chat.process.ProcessThread;

public class MainProgram {

	private static final int PORT = 6789;

	public static void main(String[] args) {
		int serverPort = args.length == 0 ? MainProgram.PORT : Integer
				.parseInt(args[0]);
		for (;;) {
			try (DatagramSocket udpSocket = new DatagramSocket(serverPort)) {
				DatagramPacket request = null;
				byte[] buffer = new byte[1024];

				System.out.println(" - Waiting for connections '"
						+ udpSocket.getLocalAddress().getHostAddress() + ":"
						+ serverPort + "' ...");

				request = new DatagramPacket(buffer, buffer.length);
				udpSocket.receive(request);
				System.out.println(" - Received a request from '"
						+ request.getAddress().getHostAddress() + ":"
						+ request.getPort() + "' -> "
						+ new String(request.getData()));
				new ProcessThread(request).start();
			} catch (SocketException e) {
				System.err.println("# UDPServer Socket error: "
						+ e.getMessage());
			} catch (IOException e) {
				System.err.println("# UDPServer IO error: " + e.getMessage());
			}
		}
	}

}
