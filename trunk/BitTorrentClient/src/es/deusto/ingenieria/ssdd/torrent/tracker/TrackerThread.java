package es.deusto.ingenieria.ssdd.torrent.tracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.bencoding.Bencoder;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.download.DownloadThread;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class TrackerThread extends Thread {

	private MetainfoFile<?> metainfo;
	private String originalUrl = null;
	private long timing = -1;
	private boolean finishedSent = false;
	private List<Peer> peersList;
	private static TrackerThread instance;
	private String myID = null;

	private TrackerThread(MetainfoFile<?> metainfo) {
		super("TrackerThread");
		this.setDaemon(false);
		this.metainfo = metainfo;
	}

	public static void startTracker(MetainfoFile<?> metainfo) {
		instance = new TrackerThread(metainfo);
		instance.start();
	}

	public static TrackerThread getInstance() {
		return instance;
	}

	@Override
	public void run() {
		super.run();
		myID = ToolKit.generatePeerId();

		if (!metainfo.getAnnounce().startsWith("http")) {
			for (int i = 0; i < metainfo.getAnnounceList().size(); i++) {
				if (metainfo.getAnnounceList().get(i).get(0).startsWith("http")) {
					originalUrl = metainfo.getAnnounceList().get(i).get(0);
					break;
				}
			}
		} else {
			originalUrl = metainfo.getAnnounce();
		}
		if (originalUrl == null) {
			throw new IllegalArgumentException("No valid announce found");
		}

		originalUrl = originalUrl + "?info_hash="
				+ metainfo.getInfo().getUrlInfoHash() + "&peer_id=" + myID
				+ "&port=" + UploadThread.port + "&compact=0" + "&no_peer_id=1";

		for (;;) {
			String url = generateUrl();

			makeConnection(url);

			// FIXME Restore
			if (timing > 0) {
				DownloadThread.getInstance().updatePeers(peersList);
			} else {
				DownloadThread.startInstance(peersList);
			}

			if (isInterrupted()) {
				break;
			}
			try {
				Thread.sleep(timing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (isInterrupted()) {
				break;
			}
		}
		finish();
	}

	private String generateUrl() {
		return generateUrl(false);
	}

	private String generateUrl(boolean stop) {
		String url = originalUrl
				+ "&uploaded="
				+ UploadThread.getInstance().getTotalBytes()
				+ "&downloaded="
				+ ((DownloadThread.getInstance() == null) ? "0"
						: DownloadThread.getInstance().getDownloadedBytes())
				+ "&left=" + FileManager.getFileManager().getRemainingSize();

		if (stop) {
			url = url + "&event=stopped";
		} else {
			if (timing <= 0) {
				url = url + "&event=started";
				if (FileManager.getFileManager().isFinished()) {
					finishedSent = true;
				}
			}
			if (FileManager.getFileManager().isFinished() && !finishedSent) {
				url = url + "&event=completed";
				finishedSent = true;
			}
		}
		return url;
	}

	private void makeConnection(String url) {
		try {
			URL connection = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection) connection
					.openConnection();
			InputStream is = urlConnection.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int i;
			while ((i = is.read()) != -1) {
				baos.write(i);
			}
			is.close();

			HashMap<String, Object> info = new Bencoder()
					.unbencodeDictionary(baos.toByteArray());
			// for (Entry<String, Object> e : info.entrySet()) {
			// System.out.println(e.getKey() + " , " + e.getValue());
			// }
			timing = (int) info.get("interval") * 1000;
			peersList = toPeerList(((String) info.get("peers")).getBytes());
			// for (Peer p : peersList) {
			// System.out.println(p);
			// }
			// peersList = getPeers((String) info.get("peers"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static List<Peer> toPeerList(byte[] data)
			throws UnknownHostException {
		List<Peer> result = new ArrayList<Peer>();
		ByteBuffer peers = ByteBuffer.wrap(data);

		for (int i = 0; i < data.length / 6; i++) {
			byte[] ipBytes = new byte[4];
			peers.get(ipBytes);
			InetAddress ip = InetAddress.getByAddress(ipBytes);
			int port = (0xFF & (int) peers.get()) << 8
					| (0xFF & (int) peers.get());
			// FIXME Send bitfield[] Check
			result.add(new Peer(ip.getHostAddress(), port, FileManager
					.getFileManager().getTotalBlocks()));
		}

		return result;
	}

	private void finish() {
		if (FileManager.getFileManager().isFinished() && !finishedSent) {
			makeConnection(generateUrl());
		}
		makeConnection(generateUrl(true));
	}

	public String getMyID() {
		return myID;
	}
}
