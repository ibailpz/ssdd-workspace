package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.InterestedMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.KeepAliveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadWorkerThreaded extends Thread implements InputListener {

	private DataInputStream in;
	private DataOutputStream out;

	private final String threadName;
	private byte[] info_hash;
	private Peer peer;
	private int block;
	private int subBlock;
	private int requestSize;
	private byte[] downloaded = null;
	private boolean requestSent = false;
	private LinkedBlockingQueue<PeerProtocolMessage> queue = new LinkedBlockingQueue<>();

	public DownloadWorkerThreaded(byte[] info_hash, Peer peer, int block,
			int subBlock, int requestSize) {
		super("DownloadWorker_" + block + "-" + subBlock + "_" + peer.getIp()
				+ ":" + peer.getPort());
		threadName = block + "-" + subBlock + "_" + peer.getIp() + ":"
				+ peer.getPort();
		this.info_hash = info_hash;
		this.peer = peer;
		this.block = block;
		this.subBlock = subBlock;
		this.requestSize = requestSize;
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

			// Send handsake
			Handsake handsake = new Handsake();
			handsake.setPeerId(TrackerThread.getInstance().getMyID());
			// handsake.setInfoHash(new String(info_hash));
			handsake.setInfoHash(info_hash);
			out.write(handsake.getBytes());
			System.out.println(this.getName() + " - Handshake sent");
			new InputThread(threadName, this, this.in).start();

			boolean finished = false;
			boolean stop = false;

			do {
				PeerProtocolMessage message = queue.take();
				stop |= handleMessage(message);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (stop) {
					finished = queue.size() == 0;
				}
			} while (!stop || !finished);

		} catch (UnknownHostException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (EOFException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(this.getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println(this.getName() + " - DownloadWorker finished");
		DownloadThread.getInstance().childFinished(block, subBlock, downloaded);
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
			if (bitfield != null && bitfield.length != 0) {
				peer.setBitfield(bitfield);
				if (!requestSent) {
					if (peer.getBitfield()[block] < 1) {
						System.out
								.println(this.getName()
										+ " - Peer does not have the block for this worker");
						return true;
					} else {
						System.out.println(this.getName()
								+ " - Peer has block " + block);
						return requestBlock();
					}
				}
			}
			return false;
		case CHOKE:
			peer.setChockedUs(true);
			System.out.println(this.getName() + " - Peer choked us");
			return true;
		case HAVE:
			HaveMsg have = (HaveMsg) message;
			int pos = ToolKit.bigEndianBytesToInt(have.getPayload(), 0);
			if (pos < 0) {
				System.out.println(this.getName()
						+ " - Peer does not have the required block");
				return true;
			}
			peer.setBitfieldPosition(pos, 1);
			if (!requestSent) {
				if (pos == block) {
					System.out.println(this.getName() + " - Peer has block "
							+ pos);
					return requestBlock();
				} else {
					System.out.println(this.getName() + " - Peer has block "
							+ pos + " but not this worker's");
					return true;
				}
			}
			return true;
		case UNCHOKE:
			peer.setChockedUs(false);
			return false;
		case PIECE:
			if (downloaded == null) {
				payload = message.getPayload();
				downloaded = new byte[payload.length - 8];
				for (int i = 0, j = 8; j < payload.length; i++, j++) {
					downloaded[i] = payload[j];
				}
				System.out.println(this.getName() + " - Piece received");
			}
			return true;
		default:
			return true;
		}
	}

	private boolean requestBlock() throws IOException {
		RequestMsg req = new RequestMsg(block, subBlock, requestSize);
		out.write(req.getBytes());
		requestSent = true;
		System.out.println(this.getName() + " - Request sent to download "
				+ requestSize + " bytes from subblock " + subBlock
				+ " from block " + block);
		return false;
	}

	@Override
	public void messageReceived(PeerProtocolMessage message) {
		try {
			System.out.println(getName() + " - Message put: "
					+ (message == null ? "null" : message.getType()));
			if (message != null) {
				queue.put(message);
			} else {
				queue.put(new KeepAliveMsg());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			this.interrupt();
		}
	}

	@Override
	public void handshakeReceived() {
		try {
			out.write(new InterestedMsg().getBytes());
			System.out.println(getName()
					+ " - Handshake received, Interested sent");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
