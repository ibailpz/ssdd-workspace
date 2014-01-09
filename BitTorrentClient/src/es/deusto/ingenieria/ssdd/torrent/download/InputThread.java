package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;

public class InputThread extends Thread {

	private static final int BUFFER_SIZE = 1024;
	private InputListener listener;
	private DataInputStream in;
	private boolean stop = false;

	public InputThread(String threadName, InputListener listener,
			DataInputStream in) {
		super("InputThread_" + threadName);
		this.listener = listener;
		this.in = in;
		System.out.println(getName() + " - InputThread started");
	}

	@Override
	public void run() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			// byte[] buffer = new byte[1024];
			byte[] buffer = readInput();
			System.out.println(getName() + " - Read handshake: "
					+ Arrays.toString(buffer));
			String bufferStr = new String(buffer);
			System.out.println(getName() + " - Read handshake string: "
					+ bufferStr);

			if (buffer.length == 0) {
				listener.messageReceived(null);
				return;
			}

			listener.handshakeReceived();

			if (buffer.length > buffer[0] + 49) {
				int length = buffer[0] + 49;
				if (bufferStr.contains("µTorrent")) {
					buffer = Arrays.copyOfRange(buffer,
							bufferStr.indexOf("e") + 2, buffer.length);
				} else {
					buffer = Arrays.copyOfRange(buffer, length, buffer.length);
				}
			}else {
				buffer = new byte[0];
			}

			System.out.println(getName() + " - Data after handshake: "
					+ Arrays.toString(buffer));

			do {
				try {
					while (buffer.length > 0) {
						PeerProtocolMessage mes = PeerProtocolMessage
								.parseMessage(buffer);
						listener.messageReceived(mes);
						if (mes != null) {
							int len = buffer.length;
							buffer = Arrays.copyOfRange(buffer,
									mes.getBytes().length, buffer.length);
							System.out.println(getName()
									+ " - Buffer length updated, from " + len
									+ " to " + buffer.length);
						} else {
							buffer = new byte[0];
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				System.out.println(getName() + " - Empty buffer. Reading...");
				buffer = readInput();
			} while (!stop);

			// ByteBuffer read = ByteBuffer.wrap(buffer);
			//
			// do {
			// while (read.hasRemaining()) {
			// PeerProtocolMessage mes = PeerProtocolMessage
			// .parseMessage(read.slice().array());
			// listener.messageReceived(mes);
			// if (mes != null) {
			// read.position(read.position() + mes.getBytes().length);
			// } else {
			// read.position(read.limit());
			// }
			// }
			// read = ByteBuffer.wrap(readInput());
			// } while (!stop);

			// do {
			// while (buffer.length > 0) {
			// PeerProtocolMessage mes = PeerProtocolMessage
			// .parseMessage(buffer);
			// listener.messageReceived(mes);
			// int mesLen = mes.getBytes().length;
			// if (buffer.length > mesLen) {
			// buffer = Arrays.copyOfRange(buffer, mesLen,
			// buffer.length);
			// }
			// }
			// buffer = readInput();
			// } while (!stop);
			//
			// do {
			// // buffer = new byte[1024];
			// buffer = readInput();
			// System.out.println(getName() + " - Read: "
			// + Arrays.toString(buffer));
			// listener.messageReceived(PeerProtocolMessage
			// .parseMessage(buffer));
			// } while (!stop);
		} catch (IOException e) {
			System.err.println(getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
		listener.messageReceived(null);
		System.out.println(getName() + " - InputThread finished");
	}

	// private void readInput(byte[] buffer) throws IOException {
	// int read = in.read(buffer);
	// buffer = Arrays.copyOf(buffer, read);
	// }

	// private byte[] readInput() throws IOException {
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// int i;
	// try {
	// while ((i = in.read()) != -1) {
	// baos.write(i);
	// }
	// } catch (SocketTimeoutException ex) {
	// }
	// return baos.toByteArray();
	// }

	private byte[] readInput() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = in.read(buffer);
		if (read < 0) {
			System.out.println(getName() + " - Input closed");
			stop = true;
			return new byte[0];
		} else if (read == BUFFER_SIZE) {
			if (in.available() > 0) {
				byte[] buffer2 = readInput();
				byte[] dest = new byte[buffer.length + buffer2.length];
				System.arraycopy(buffer, 0, dest, 0, buffer.length);
				System.arraycopy(buffer2, 0, dest, buffer.length,
						buffer2.length);
				buffer = dest;
			}
		} else {
			buffer = Arrays.copyOf(buffer, read);
		}
		System.out.println(getName() + " - " + buffer.length + " bytes read");
		return buffer;
	}

	@Override
	public void interrupt() {
		stop = true;
		super.interrupt();
	}

}
