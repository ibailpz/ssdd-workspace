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
import es.deusto.ingenieria.ssdd.torrent.main.WindowManager;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class TrackerThread extends Thread {

	public static final int subBlockSize = 32;

	private MetainfoFile<?> metainfo;
	private boolean started = false;
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

		System.out.println("TrackerThread - TrackerThread started");

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
		System.out.println("TrackerThread - Tracker: " + originalUrl);

		originalUrl = originalUrl + "?info_hash="
				+ metainfo.getInfo().getUrlInfoHash() + "&peer_id=" + myID
				+ "&port=" + UploadThread.port + "&compact=0" + "&no_peer_id=1";

		for (;;) {
			String url = generateUrl();

			try {
				makeConnection(url);
			} catch (IOException e1) {
				System.err.println(getName() + " - " + e1.getMessage());
				e1.printStackTrace();
				if (!started) {
					WindowManager
							.getInstance()
							.displayError(
									"Cannot contact tracker. Please check internet connection and try again later");
				}
				break;
			}

			if (started) {
				DownloadThread.getInstance().updatePeers(peersList);
			} else {
				DownloadThread.startInstance(peersList);
				started = true;
			}

			if (isInterrupted()) {
				break;
			}
			try {
				Thread.sleep(timing);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			if (Thread.interrupted()) {
				break;
			}
		}
		finish();
		System.out.println("TrackerThread - TrackerThread stopped");
		WindowManager.exitBlocker.release();
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
			System.out.println("TrackerThread - TrackerThread stopping...");
			url = url + "&event=stopped";
		} else {
			if (!started) {
				url = url + "&event=started";
				if (FileManager.getFileManager().isFinished()) {
					finishedSent = true;
				}
			}
			if (FileManager.getFileManager().isFinished() && !finishedSent) {
				System.out.println("TrackerThread - Send completed");
				url = url + "&event=completed";
				finishedSent = true;
			}
		}
		return url;
	}

	private void makeConnection(String url) throws IOException {
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

		HashMap<String, Object> info = new Bencoder().unbencodeDictionary(baos
				.toByteArray());
		// for (Entry<String, Object> e : info.entrySet()) {
		// System.out.println(e.getKey() + " , " + e.getValue());
		// }
		timing = (int) info.get("interval") * 1000;
		System.out.println("TrackerThread - New timing: " + timing);
		peersList = toPeerList(info.get("peers"));
		System.out.println("Peers obtained: " + peersList.toString());
		// for (Peer p : peersList) {
		// System.out.println(p);
		// }
		// peersList = getPeers((String) info.get("peers"));
	}

	@SuppressWarnings("unchecked")
	private static List<Peer> toPeerList(Object o) throws UnknownHostException {
		List<Peer> result = null;
		if (o instanceof String) {
			byte[] data = ((String) o).getBytes();
			result = new ArrayList<Peer>();
			ByteBuffer peers = ByteBuffer.wrap(data);

			for (int i = 0; i < data.length / 6; i++) {
				byte[] ipBytes = new byte[4];
				peers.get(ipBytes);
				InetAddress ip = InetAddress.getByAddress(ipBytes);
				int port = (0xFF & (int) peers.get()) << 8
						| (0xFF & (int) peers.get());
				addPeer(result, ip.getHostAddress(), port);
			}
		} else {
			result = new ArrayList<>();
			ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) o;
			for (HashMap<String, Object> map : list) {
				addPeer(result, map.get("ip").toString(),
						Integer.parseInt(map.get("port").toString()));
			}
		}
		return result;
	}

	private static void addPeer(List<Peer> list, String ip, int port) {
		if ((!ip.equals("127.0.0.1") && !ip.equals("::1"))
				|| port != UploadThread.port) {
			Peer p = new Peer(ip, port, FileManager.getFileManager()
					.getTotalBlocks());
			list.add(p);
		}
	}

	private void finish() {
		System.out.println("TrackerThread - Finish requested");
		if (FileManager.getFileManager().isFinished() && !finishedSent) {
			try {
				makeConnection(generateUrl());
			} catch (IOException e) {
				System.err.println(getName() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
		try {
			makeConnection(generateUrl(true));
		} catch (IOException e) {
			System.err.println(getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
	}

	public String getMyID() {
		return myID;
	}
}