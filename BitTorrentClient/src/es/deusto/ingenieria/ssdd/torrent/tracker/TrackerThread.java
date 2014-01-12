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
	private String baseUrl = null;
	private long timing = -1;
	private boolean finishedSent = false;
	private List<Peer> peersList;
	private static TrackerThread instance;
	private String myID = null;
	private int trackerIndex = -1;

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

		if (metainfo.getAnnounce().startsWith("http")) {
			baseUrl = metainfo.getAnnounce();
			baseUrl = baseUrl + "?info_hash="
					+ metainfo.getInfo().getUrlInfoHash() + "&peer_id=" + myID
					+ "&port=" + UploadThread.port + "&compact=0"
					+ "&no_peer_id=1";
		} else {
			setNewTracker();
		}
		if (baseUrl == null) {
			throw new IllegalArgumentException("No valid announce found");
		}
		System.out.println("TrackerThread - Tracker: " + baseUrl);

		for (;;) {
			boolean connected = false;
			do {
				String url = generateUrl();
				try {
					makeConnection(url);
					connected = true;
				} catch (IOException e1) {
					System.err.println(getName() + " - " + e1.getMessage());
					e1.printStackTrace();
					if (!started) {
						setNewTracker();
						if (baseUrl == null) {
							WindowManager
									.getInstance()
									.displayError(
											"Cannot contact any tracker. Please check internet connection and try again later");
							break;
						}
					}
				}
			} while (!connected);

			if (!connected) {
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
		if (baseUrl != null) {
			finish();
		}
		System.out.println("TrackerThread - TrackerThread stopped");
		WindowManager.exitBlocker.release();
	}

	private void setNewTracker() {
		trackerIndex++;
		baseUrl = null;
		if (metainfo.getAnnounceList() != null
				&& metainfo.getAnnounceList().get(0) != null) {
			for (; trackerIndex < metainfo.getAnnounceList().get(0).size(); trackerIndex++) {
				if (metainfo.getAnnounceList().get(0).get(trackerIndex)
						.startsWith("http")) {
					baseUrl = metainfo.getAnnounceList().get(0)
							.get(trackerIndex);
					baseUrl = baseUrl + "?info_hash="
							+ metainfo.getInfo().getUrlInfoHash() + "&peer_id="
							+ myID + "&port=" + UploadThread.port
							+ "&compact=0" + "&no_peer_id=1";
					System.out
							.println(getName() + " - New tracker: " + baseUrl);
					break;
				}
			}
		}
	}

	private String generateUrl() {
		return generateUrl(false);
	}

	private String generateUrl(boolean stop) {
		String url = baseUrl
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
		timing = (int) info.get("interval") * 1000;
		System.out.println("TrackerThread - New timing: " + timing);
		peersList = toPeerList(info.get("peers"));
		System.out.println("Peers obtained: " + peersList.toString());
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
