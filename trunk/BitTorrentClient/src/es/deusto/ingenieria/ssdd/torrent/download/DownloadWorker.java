package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.InterestedMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadWorker extends Thread {

	private DataInputStream in;
	private DataOutputStream out;

	private byte[] info_hash;
	private Peer peer;
	private int block;
	private int subBlock;
	private byte[] downloaded = null;

	public DownloadWorker(byte[] info_hash, Peer peer, int block, int subBlock) {
		super("DownloadWorker_" + block + "-" + subBlock + "_" + peer.getIp()
				+ ":" + peer.getPort());
		this.info_hash = info_hash;
		this.peer = peer;
		this.block = block;
		this.subBlock = subBlock;
	}

	@Override
	public void run() {
		super.run();
		System.out.println(this.getName() + " - DownloadWorker started");

		try (Socket tcpSocket = new Socket(peer.getIp(), peer.getPort());
				DataInputStream in = new DataInputStream(
						tcpSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(
						tcpSocket.getOutputStream());) {

			this.in = in;
			this.out = out;
			tcpSocket.setSoTimeout(1000);

			// Send handsake
			Handsake handsake = new Handsake();
			handsake.setPeerId(TrackerThread.getInstance().getMyID());
			// handsake.setInfoHash(new String(info_hash));
			handsake.setInfoHash(info_hash);
			out.write(handsake.getBytes());
			System.out.println(this.getName() + " - Handshake sent: "
					+ new String(handsake.getBytes()));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Read handshake and ignore
			int length = in.read();
			// byte[] read = new byte[68];
			// byte[] read = new byte[74];
			// byte[] read = new byte[48 + length];
			byte[] read = new byte[73];
			in.read(read);
			System.out.println(this.getName() + " - Handshake read (" + length
					+ "+49" + "): " + new String(read));

			read = readInput();
			PeerProtocolMessage mes = PeerProtocolMessage.parseMessage(read);
			boolean isFinished = false;
			if (mes == null) {
				System.out.println(this.getName() + " - Read: "
						+ new String(read));
				System.out.println(this.getName() + " - Read: "
						+ Arrays.toString(read));
				// HashMap<String, Object> map = new Bencoder()
				// .unbencodeDictionary(read);
				// for (String s : map.keySet()) {
				// System.out.println(s + ":" + map.get(s));
				// }
				// 135 in the middle of e
				// 136 to end e
				// read = Arrays.copyOfRange(read, 135, read.length);
				read = Arrays.copyOfRange(read,
						new String(read).indexOf("e") + 2, read.length);
				mes = PeerProtocolMessage.parseMessage(read);
				System.out.println(mes.getPayload().length);
				System.out.println(FileManager.getFileManager()
						.getTotalBlocks());
				System.out.println(Arrays.toString(mes.getPayload()));
				if (mes.getPayload().length != FileManager.getFileManager()
						.getTotalBlocks()) {
					mes.setPayload(Arrays.copyOf(mes.getPayload(), FileManager
							.getFileManager().getTotalBlocks()));
				}
				out.write(new InterestedMsg().getBytes());
				System.out.println(this.getName() + " - Interested sent");
				isFinished = handleMessage(PeerProtocolMessage
						.parseMessage(readInput()));
				if (!isFinished) {
					isFinished = handleMessage(mes);
				}
				// if (bm.getPayload().length > FileManager.getFileManager()
				// .getBitfield().length) {
				// bm = (BitfieldMsg) PeerProtocolMessage.parseMessage(Arrays
				// .copyOf(bm.getPayload(), FileManager
				// .getFileManager().getBitfield().length));
				// System.out.println("Bitfield: "
				// + Arrays.toString(bm.getPayload()));
				// System.out.println(PeerProtocolMessage.parseMessage(Arrays
				// .copyOfRange(read, bm.getBytes().length,
				// read.length)));
				// } else {
				// isFinished = handleMessage(bm);
				// }
				// out.write(new InterestedMsg().getBytes());
				// System.out.println(this.getName() + " - Interested sent");
			} else {
				isFinished = handleMessage(mes);
			}

			while (!isFinished) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				PeerProtocolMessage message = PeerProtocolMessage
						.parseMessage(readInput());
				System.out.println(this.getName() + " - Message read: "
						+ (message == null ? "null" : message.getType()));
				// Check the message
				isFinished = handleMessage(message);
			}

		} catch (UnknownHostException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (EOFException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println(this.getName() + " - DownloadWorker finished");
		DownloadThread.getInstance().childFinished(block, subBlock, downloaded);
	}

	private byte[] readInput() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i;
		try {
			while ((i = in.read()) != -1) {
				baos.write(i);
			}
		} catch (SocketTimeoutException ex) {
		}
		// System.out.println(this.getName() + " - "
		// + Arrays.toString(baos.toByteArray()));
		// System.out.println(this.getName() + " - "
		// + new String(baos.toByteArray()));
		return baos.toByteArray();
	}

	private boolean handleMessage(PeerProtocolMessage message)
			throws IOException {
		System.out.println(this.getName() + " - Handling message "
				+ (message == null ? "null" : message.getType()));
		if (message == null) {
			return true;
		}
		byte[] payload;
		switch (message.getType()) {
		case BITFIELD:
			payload = ((BitfieldMsg) message).getPayload();
			int[] bitfield = new int[payload.length];
			for (int j = 0; j < payload.length; j++) {
				if (payload[j] > 0)
					bitfield[j] = 1;
				else
					bitfield[j] = 0;

			}
			peer.setBitfield(bitfield);
			if (peer.getBitfield()[block] < 1) {
				System.out.println(this.getName()
						+ " - Peer does not have the block for this worker");
				return true;
			}
			System.out.println(this.getName() + " - Peer has block " + block);
			return requestBlock();
		case CHOKE:
			peer.setChockedUs(true);
			System.out.println(this.getName() + " - Peer choked us");
			return true;
		case HAVE:
			HaveMsg have = (HaveMsg) message;
			int pos = ToolKit.bigEndianBytesToInt(have.getBytes(), 0);
			if (pos < 0) {
				System.out.println(this.getName()
						+ " - Peer does not have the required block");
				return true;
			}
			peer.setBitfieldPosition(pos, 1);
			if (pos == block) {
				System.out.println(this.getName() + " - Peer has block " + pos);
				return requestBlock();
			} else {
				System.out.println(this.getName() + " - Peer has block " + pos
						+ " but not this worker's");
				return true;
			}
		case UNCHOKE:
			peer.setChockedUs(false);
			// out.write(new UnChokeMsg().getBytes());
			// System.out.println(this.getName()
			// + " - Peer unchoked us, unchoke sent");
			// requestBlock();
			return false;
		case PIECE:
			payload = message.getPayload();
			downloaded = new byte[payload.length - 8];
			for (int i = 0, j = 8; j < payload.length; i++, j++) {
				downloaded[i] = payload[j];
			}
			System.out.println(this.getName() + " - Piece received");
			return true;
		default:
			return true;
		}
	}

	private boolean requestBlock() throws IOException {
		// try {
		// Thread.sleep(500);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		RequestMsg req = new RequestMsg(block, subBlock,
				TrackerThread.subBlockSize);
		out.write(req.getBytes());
		System.out.println(this.getName()
				+ " - Request sent to download subblock " + subBlock
				+ " from block " + block);
		return false;
		// downloaded = readInput();
		// PeerProtocolMessage message = PeerProtocolMessage
		// .parseMessage(downloaded);
		// if (message == null) {
		// peer.setBitfieldPosition(block, 1);
		// return true;
		// } else {
		// downloaded = null;
		// handleMessage(message);
		// return false;
		// }
	}

}
