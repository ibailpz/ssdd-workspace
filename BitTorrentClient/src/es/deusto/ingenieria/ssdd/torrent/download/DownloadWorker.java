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
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;

public class DownloadWorker extends Thread {

	private String info_hash;
	private Peer peer;
	private int block;
	private int subBlock;

	// TODO Completar parametros
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

			// Send handsake
			Handsake handsake = new Handsake();
			handsake.setPeerId(TrackerThread.getInstance().getMyID());
			handsake.setInfoHash(info_hash);
			out.write(handsake.getBytes());

			// Read peer id and ignore
			in.read(new byte[20]);

			boolean isFinished = false;
			while (!isFinished) {
				PeerProtocolMessage message = readMessage(in);
				// Check the message
				isFinished = handleMessage(message, out);
			}

		} catch (UnknownHostException e) {
			System.err.println("# TCPClient Socket error: " + e.getMessage());
		} catch (EOFException e) {
			System.err.println("# TCPClient EOF error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("# TCPClient IO error: " + e.getMessage());
		}
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

	private boolean handleMessage(PeerProtocolMessage message, DataOutputStream out) {
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
			return false;
		case CHOKE:

			return true;
		case HAVE:

			return false;
		case UNCHOKE:

			return false;
		default:
			return true;
		}
	}	

}
