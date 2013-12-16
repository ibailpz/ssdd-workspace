package es.deusto.ingenieria.ssdd.torrent.tracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.bencoding.Bencoder;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;
import es.deusto.ingenieria.ssdd.torrent.data.Peer;
import es.deusto.ingenieria.ssdd.torrent.download.DownloadThread;
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

			// TODO asignar parametros a la url
			url = url + "?info_hash=" + metainfo.getInfo().getHexInfoHash()
					+ "&peer_id=" + ToolKit.generatePeerId() + "&port="
					+ UploadThread.port + "&uploaded="
					+ UploadThread.getInstance().getTotalBytes()
					+ "&downloaded="
					+ DownloadThread.getInstance().getDownloadedBytes()
					+ "&left=" + "&compact=" + "&no_peer_id=" + "&event=";

			try {
				connection = new URL(url);
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}

			try {
				urlConnection = connection.openConnection();
				InputStream is = urlConnection.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int i;
				while ((i = is.read()) != -1) {
					baos.write(i);
				}
				is.close();

				HashMap<String, Object> info = bencoder
						.unbencodeDictionary(baos.toByteArray());
				timing = (long) info.get("interval");
				peersList = (List<Peer>) info.get("peers");

			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if (timing > 0) {
				DownloadThread.getInstance().updatePeers(peersList);
			} else {
				DownloadThread.startInstance(peersList);
			}

			try {
				Thread.sleep(timing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
