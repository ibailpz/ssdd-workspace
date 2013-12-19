package es.deusto.ingenieria.ssdd.torrent.tracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import es.deusto.ingenieria.ssdd.bitTorrent.bencoding.Bencoder;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class TrackerThread extends Thread {

	private MetainfoFile<?> metainfo;
	private long timing = -1;
	private List<Peer> peersList;
	private static TrackerThread instance;

	private TrackerThread(MetainfoFile<?> metainfo) {
		super();
		this.metainfo = metainfo;
	}

	public static void startTracker(MetainfoFile<?> metainfo) {
		instance = new TrackerThread(metainfo);
		instance.start();
	}

	public static TrackerThread getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		super.run();

		String url = null;
		URL connection = null;
		URLConnection urlConnection = null;

		if (!metainfo.getAnnounce().startsWith("http")) {
			for (int i = 0; i < metainfo.getAnnounceList().size(); i++) {
				if (metainfo.getAnnounceList().get(i).get(0).startsWith("http")) {
					url = metainfo.getAnnounceList().get(i).get(0);
					break;
				}
			}
		} else {
			url = metainfo.getAnnounce();
		}

		for (;;) {
			Bencoder bencoder = new Bencoder();

			url = url + "?info_hash=" + metainfo.getInfo().getUrlInfoHash()
					+ "&peer_id=" + ToolKit.generatePeerId() + "&port="
					+ UploadThread.port + "&uploaded=" + 0 + "&downloaded=" + 0
					+ "&left="
					+ FileManager.getFileManager().getRemainingSize()
					+ "&compact=0" + "&no_peer_id=1";

			if (timing <= 0) {
				url = url + "&event=started";
			}

			try {
				connection = new URL(url);
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}

			try {
				urlConnection = connection.openConnection();
				System.out.println(url.toString());
				InputStream is = urlConnection.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int i;
				while ((i = is.read()) != -1) {
					baos.write(i);
				}
				is.close();

				HashMap<String, Object> info = bencoder
						.unbencodeDictionary(baos.toByteArray());
				for (Entry<String, Object> e : info.entrySet()) {
					System.out.println(e.getKey() + " , " + e.getValue());
				}
				timing = (int) info.get("interval") * 1000;
				System.out.println(timing);
				peersList = getPeers((String) info.get("peers"));
				System.out.println(peersList.size());
				System.out.println(info.size());

			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// if (timing > 0) {
			// DownloadThread.getInstance().updatePeers(peersList);
			// } else {
			// DownloadThread.startInstance(peersList);
			// }

			try {
				Thread.sleep(timing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Peer> getPeers(String peers) {
		ArrayList<Peer> peerList = new ArrayList<Peer>();
		for (int i = 0; i < peers.length(); i = i + 12) {
			String ip = "";
			for (int j = 0; j < 8; j = j + 2) {
				ip = ip + Integer.parseInt(peers.substring(i, i + 2), 16);
				ip = ip + ".";
			}
			ip = ip.substring(0, ip.length()-1);
			int port = Integer.parseInt(peers.substring(8, 12), 16);
			System.out.println(ip + ":" + port);
			peerList.add(new Peer(ip, port));
		}

		return peerList;
	}
}
