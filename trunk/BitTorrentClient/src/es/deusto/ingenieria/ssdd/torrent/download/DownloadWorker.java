package es.deusto.ingenieria.ssdd.torrent.download;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadWorker extends Thread {

	private DataInputStream in;
	private DataOutputStream out;

	private String info_hash;
	private Peer peer;
	private int block;
	private int subBlock;
	private byte[] downloaded = null;

	public DownloadWorker(String info_hash, Peer peer, int block, int subBlock) {
		this.info_hash = info_hash;
		this.peer = peer;
		this.block = block;
		this.subBlock = subBlock;
	}

	@Override
	public void run() {
		super.run();

		try (Socket tcpSocket = new Socket(peer.getIp(), peer.getPort());
				DataInputStream in = new DataInputStream(
						tcpSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(
						tcpSocket.getOutputStream())) {

			this.in = in;
			this.out = out;

			// Send handsake
			Handsake handsake = new Handsake();
			handsake.setPeerId(TrackerThread.getInstance().getMyID());
			handsake.setInfoHash(info_hash);
			out.write(handsake.getBytes());

			// Read peer id and ignore
			in.read(new byte[20]);

			boolean isFinished = false;
			while (!isFinished) {
				PeerProtocolMessage message = PeerProtocolMessage.parseMessage(readInput());
				// Check the message
				isFinished = handleMessage(message);
			}

		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
		}
		DownloadThread.getInstance().childFinished(block, subBlock, downloaded);
	}

	private byte[] readInput() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i;
		while ((i = in.read()) != -1) {
			baos.write(i);
		}
		return baos.toByteArray();
	}

	private boolean handleMessage(PeerProtocolMessage message)
			throws IOException {
		switch (message.getType()) {
		case BITFIELD:
			byte[] payload = ((BitfieldMsg) message).getPayload();
			int[] bitfield = new int[payload.length];
			for (int j = 0; j < payload.length; j++) {
				if (payload[j] > 0)
					bitfield[j] = 1;
				else
					bitfield[j] = 0;

			}
			peer.setBitfield(bitfield);
			if (peer.getBitfield()[block] < 1) {
				return true;
			}
			return downloadBlock();
		case CHOKE:
			peer.setChockedUs(true);
			return true;
		case HAVE:
			HaveMsg have = (HaveMsg) message;
			int pos = ToolKit.bigEndianBytesToInt(have.getBytes(), 0);
			if (pos < 0) {
				return true;
			}
			peer.setBitfieldPosition(pos, 1);
			return downloadBlock();
		case UNCHOKE:
			peer.setChockedUs(false);
			return false;
		default:
			return true;
		}
	}

	private boolean downloadBlock() throws IOException {
		RequestMsg req = new RequestMsg(block, subBlock, 32);
		out.write(req.getBytes());
		downloaded = readInput();
		return true;
	}

}
