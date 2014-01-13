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
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.UnChokeMsg;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class UploadWorker extends Thread {

	private static final int BUFFER_SIZE = 1024;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket tcpSocket;
	private int dataSent = 0;
	private boolean isFinished = false;

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
			int length = in.read();
			byte[] bytes = new byte[48 + length]; // 49 of handshake
													// + read length
													// - 1 byte read
													// (usually 68 in total)
			in.read(bytes);
			byte[] info_hash = new byte[20];
			ByteBuffer handshake = ByteBuffer.wrap(bytes);
			handshake.get(new byte[length + 8]); // Read length
													// + 8 zeros
													// (usually 28 'til here)
			handshake.get(info_hash);
			System.out.println(this.getName() + " - Handshake read");
			System.out.println("\tOriginal: "
					+ Arrays.toString(FileManager.getFileManager()
							.getInfoHash()));
			System.out.println("\tRead:     " + Arrays.toString(info_hash));
			System.out.println("\t          " + Arrays.toString(bytes));
			
			System.out.println("\tOriginal: "
					+ new String(FileManager.getFileManager().getInfoHash()));
			System.out.println("\tRead:     " + new String(info_hash));
			System.out.println("\t          " + new String(bytes));

			// if (new String(FileManager.getFileManager().getInfoHash())
			// .equals(new String(info_hash))) {
			if (Arrays.equals(FileManager.getFileManager().getInfoHash(),
					info_hash)) {

				System.out.println(this.getName() + " - Handshake correct");

				// Answer with handshake
				Handsake handsake = new Handsake();
				handsake.setPeerId(TrackerThread.getInstance().getMyID());
				handsake.setInfoHash(FileManager.getFileManager().getInfoHash());
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

				// Timer and TimerTask
				TimerTask timerTask;
				Timer timer = new Timer();

				while (!isFinished && !isInterrupted()) {
					timerTask = new TimerTask() {
						@Override
						public void run() {
							isFinished = true;
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
						isFinished |= handleMessage(message);
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
					} while (!isFinished && buffer.length > 0
							&& !isInterrupted());
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

	private boolean handleMessage(PeerProtocolMessage message)
			throws IOException {
		System.out.println(this.getName() + " - Handling message "
				+ (message == null ? "null" : message.getType()));
		if (message == null) {
			return true;
		}
		switch (message.getType()) {
		case REQUEST:
			RequestMsg req = (RequestMsg) message;
			int indexRequest = req.getIndex();
			int begin = req.getBegin();
			int length = req.getRequestedLength();

			byte[] block = FileManager.getFileManager().getBlock(indexRequest);
			if (block == null) {
				System.out.println(this.getName()
						+ " - Invalid block requested");
				return true;
			} else {
				if (block.length - begin < length) {
					System.out.println(this.getName()
							+ " - Invalid block length requested");
					return true;
				}
				byte[] send = new byte[length];
				for (int i = begin, j = 0; i < block.length && j < length; i++, j++) {
					send[j] = block[i];
				}
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