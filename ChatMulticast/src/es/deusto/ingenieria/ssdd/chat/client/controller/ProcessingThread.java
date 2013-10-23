package es.deusto.ingenieria.ssdd.chat.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class ProcessingThread extends Thread {

	private String ip;
	private int port;
	private ChatClientController controller;
	private boolean stop = false;

	public ProcessingThread(String ip, int port, ChatClientController c) {
		this.ip = ip;
		this.port = port;
		controller = c;
	}

	@Override
	public void run() {
		for (; !stop;) {
			try (MulticastSocket socket = new MulticastSocket(port)) {
				InetAddress group = InetAddress.getByName(ip);
				socket.joinGroup(group);

				for (; !stop;) {
					byte[] buffer = new byte[1024];
					final DatagramPacket messageIn = new DatagramPacket(buffer,
							buffer.length);

					socket.receive(messageIn);

					if (stop) {
						break;
					}

					System.out.println(" - Received a message from '"
							+ messageIn.getAddress().getHostAddress() + ":"
							+ messageIn.getPort() + "' -> "
							+ new String(messageIn.getData()).trim());

					new Thread(new Runnable() {
						
						@Override
						public void run() {
							controller.processRequest(messageIn);
						}
					}).start();
				}
				socket.leaveGroup(group);
			} catch (SocketException e) {
				System.err.println("# Socket Error: " + e.getMessage());
			} catch (IOException e) {
				System.err.println("# IO Error: " + e.getMessage());
			}
		}
	}

	@Override
	public void interrupt() {
		stop = true;
//		super.interrupt();
	}

}
