package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.UnChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class UploadWorker extends Thread {

	private static final int BUFFER_SIZE = 1024;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket tcpSocket;
	private int dataSent = 0;

	public UploadWorker(Socket socket) {
		super("UploadWorker_" + socket.getInetAddress().getHostAddress() + ":"
				+ socket.getPort());
		System.out.println("UploadThread - UploadThread started");
		try {
			this.tcpSocket = socket;
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("# TCPConnection IO error:" + e.getMessage());
		}
	}

	public void run() {
		System.out.println(this.getName() + " - UploadWorker started");
		try {
			byte[] bytes = new byte[68];
			in.read(bytes);
			byte[] info_hash = new byte[20];
			ByteBuffer handshake = ByteBuffer.wrap(bytes);
			handshake.get(new byte[28]);
			handshake.get(info_hash);
			System.out.println(this.getName() + " - Handshake read: "
					+ new String(info_hash) + "; ours: "
					+ new String(FileManager.getFileManager().getInfoHash()));

			if (new String(FileManager.getFileManager().getInfoHash())
					.equals(new String(info_hash))) {

				System.out.println(this.getName() + " - Handshake correct");

				// Answer with handshake
				Handsake handsake = new Handsake();
				handsake.setPeerId(TrackerThread.getInstance().getMyID());
				handsake.setInfoHash(new String(FileManager.getFileManager()
						.getInfoHash()));
				out.write(handsake.getBytes());
				out.flush();
				System.out.println(this.getName() + " - Handshake sent");

				// Send bitfield
				BitfieldMsg bitfield = new BitfieldMsg(FileManager
						.getFileManager().getBitfield());
				out.write(bitfield.getBytes());
				out.flush();
				System.out.println(this.getName() + " - Send bitfield: "
						+ Arrays.toString(bitfield.getBytes()));

				// // Timer and TimerTask
				// TimerTask timerTask;
				// Timer timer = new Timer();
				// boolean isFinished = false;
				//
				// while (!isFinished) {
				// timerTask = new TimerTask() {
				// @Override
				// public void run() {
				// UploadWorker.this.interrupt();
				// System.out.println(UploadWorker.this.getName()
				// + " - Timeout");
				// }
				// };
				// timer.schedule(timerTask, 120000);
				// PeerProtocolMessage message = readMessage(in);
				// System.out.println(this.getName() + " - Message read: "
				// + (message == null ? "null" : message.getType()));
				// timerTask.cancel();
				// isFinished = handleMessage(message, out);
				// }

				// Timer and TimerTask
				TimerTask timerTask;
				Timer timer = new Timer();
				boolean isFinished = false;

				while (!isFinished) {
					timerTask = new TimerTask() {
						@Override
						public void run() {
							UploadWorker.this.interrupt();
							System.out.println(UploadWorker.this.getName()
									+ " - Timeout");
						}
					};
					timer.schedule(timerTask, 120000);

					byte[] buffer = readInput();
					timerTask.cancel();

					do {
						PeerProtocolMessage message = PeerProtocolMessage
								.parseMessage(buffer);
						System.out
								.println(this.getName()
										+ " - Message read: "
										+ (message == null ? "null" : message
												.getType()));
						isFinished = handleMessage(message);
						if (message != null) {
							int len = buffer.length;
							buffer = Arrays.copyOfRange(buffer,
									message.getBytes().length, buffer.length);
							System.out.println(getName()
									+ " - Buffer length updated, from " + len
									+ " to " + buffer.length);
						} else {
							buffer = new byte[0];
						}
					} while (buffer.length > 0);
				}
			} else {
				System.out
						.println(getName() + " - Wrong hash, drop connection");
			}
		} catch (EOFException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				tcpSocket.close();
			} catch (IOException e) {
				System.err.println(this.getName() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println(this.getName() + " - UploadWorker finished");
		UploadThread.getInstance().childFinished(dataSent);
	}

	private byte[] readInput() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = in.read(buffer);
		if (read < 0) {
			return null;
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

	// private PeerProtocolMessage readMessage(DataInputStream in)
	// throws IOException {
	//
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// int i;
	// while ((i = in.read()) != -1) {
	// baos.write(i);
	// }
	//
	// return PeerProtocolMessage.parseMessage(baos.toByteArray());
	// }

	private boolean handleMessage(PeerProtocolMessage message)
			throws IOException {
		System.out.println(this.getName() + " - Handling message "
				+ (message == null ? "null" : message.getType()));
		if (message == null) {
			return true;
		}
		switch (message.getType()) {
		// case PIECE:
		// byte[] bitfield = FileManager.getFileManager().getBitfield();
		// byte[] piece = message.getPayload();
		// int index = ToolKit.bigEndianBytesToInt(piece, 0);
		//
		// if (bitfield[index] == 1) {
		// System.out.println(this.getName() + " - Sending correct HAVE");
		// HaveMsg haveMsg = new HaveMsg(index);
		// out.write(haveMsg.getBytes());
		// } else {
		// System.out.println(this.getName() + " - No block, -1 HAVE");
		// HaveMsg haveMsg = new HaveMsg(-1);
		// out.write(haveMsg.getBytes());
		// }
		// return false;
		case REQUEST:
			byte[] request = message.getPayload();
			int indexRequest = ToolKit.bigEndianBytesToInt(request, 0);
			int begin = ToolKit.bigEndianBytesToInt(request, 4);
			int length = ToolKit.bigEndianBytesToInt(request, 8);

			byte[] block = FileManager.getFileManager().getBlock(indexRequest);
			if (block == null) {
				System.out.println(this.getName()
						+ " - Invalid block requested");
				return true;
			} else {
				byte[] send = new byte[length];
				for (int i = begin, j = 0; i < block.length && j < length; i++, j++) {
					send[j] = block[i];
				}
				// out.write(send);
				PieceMsg ans = new PieceMsg(indexRequest, begin, send);
				out.write(ans.getBytes());
				System.out.println(this.getName() + " - " + length
						+ " bytes subset from " + begin + " offset at block "
						+ indexRequest + " sent");
				dataSent += length;
				return false;
			}
		case CANCEL:
			return true;
		case INTERESTED:
			out.write(new UnChokeMsg().getBytes());
			return false;
		case KEEP_ALIVE:
			return false;
		case NOT_INTERESTED:
			return true;
		default:
			return true;
		}
	}
}