package es.deusto.ingenieria.ssdd.torrent.upload;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

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
		try {
			// TODO Read handshake
			byte[] bytes = new byte[68];
			in.read(bytes);

			// Answer with my id
			out.write(TrackerThread.getInstance().getMyID().getBytes());
			// Answer with my bitfield
			BitfieldMsg bitfield = new BitfieldMsg(FileManager.getFileManager()
					.getBitfield());
			out.write(bitfield.getBytes());

			// Timer and TimerTask
			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					UploadWorker.this.interrupt();
				}
			};

			Timer timer = new Timer();
			boolean isFinished = false;

			while (!isFinished) {
				timerTask.cancel();
				timerTask = new TimerTask() {
					@Override
					public void run() {
						UploadWorker.this.interrupt();
					}
				};
				timer.schedule(timerTask, 120000);
				PeerProtocolMessage message = readMessage(in);
				isFinished = handleMessage(message, out);
			}

		} catch (EOFException e) {
			System.err.println("# TCPConnection EOF error" + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPConnection IO error:" + e.getMessage());
		} catch (Exception e) {
			System.err.println("# TCPConnection error:" + e.getMessage());
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

	private PeerProtocolMessage readMessage(DataInputStream in)
			throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i;
		while ((i = in.read()) != -1) {
			baos.write(i);
		}

		return PeerProtocolMessage.parseMessage(baos.toByteArray());
	}

	private boolean handleMessage(PeerProtocolMessage message,
			DataOutputStream out) {
		switch (message.getType()) {
		case BITFIELD:

			return false;
		case CANCEL:

			return true;
		case CHOKE:

			return true;
		case HAVE:
			
			return false;
		case INTERESTED:

			return false;
		case KEEP_ALIVE:

			return false;
		case NOT_INTERESTED:

			return true;
		case PIECE:
			
			return false;
		case PORT:

			return false;
		case REQUEST:

			return false;
		case UNCHOKE:

			return false;
		default:
			return true;
		}
	}
}