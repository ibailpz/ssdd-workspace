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
			byte[] bytes = new byte[68];
			in.read(bytes);
			byte[] info_hash = new byte[20];
			for(int i=28, j=0; j<info_hash.length; i++, j++) {
				info_hash[j] = bytes[i];
			}

			if (FileManager.getFileManager().getInfoHash()
					.equals(new String(info_hash))) {

				// Answer with my id
				out.write(TrackerThread.getInstance().getMyID().getBytes());
				// Answer with my bitfield
				BitfieldMsg bitfield = new BitfieldMsg(FileManager
						.getFileManager().getBitfield());
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
			DataOutputStream out) throws IOException {
		switch (message.getType()) {
		case PIECE:
			byte[] bitfield = FileManager.getFileManager().getBitfield();
			byte[] piece = message.getPayload();
			int index = ToolKit.bigEndianBytesToInt(piece, 0);

			if (bitfield[index] == 1) {
				HaveMsg haveMsg = new HaveMsg(index);
				out.write(haveMsg.getBytes());
			} else {
				HaveMsg haveMsg = new HaveMsg(-1);
				out.write(haveMsg.getBytes());
			}
			return false;
		case REQUEST:
			byte[] request = message.getPayload();
			int indexRequest = ToolKit.bigEndianBytesToInt(request, 0);
			int begin = ToolKit.bigEndianBytesToInt(request, 4);
			int length = ToolKit.bigEndianBytesToInt(request, 8);

			byte[] block = FileManager.getFileManager().getBlock(indexRequest);
			byte[] send = new byte[length];
			for (int i = begin, j = 0; j < length; i++, j++) {
				send[j] = block[i];
			}
			out.write(send);
			return false;
		case CANCEL:
			return true;
		case INTERESTED:
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