package es.deusto.ingenieria.ssdd.torrent.tracker;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.torrent.download.DownloadThread;

public class TrackerTheard extends Thread {

	private MetainfoFile<?> metainfo;
	private long timing = -1;

	public TrackerTheard(MetainfoFile<?> metainfo) {
		super();
		this.metainfo = metainfo;
	}

	@Override
	public void run() {
		super.run();

		for (;;) {
			
			// TODO abrir conexion con el tracker con parametros
			// recoger timing y lista de peers

			if (timing > 0) {
				// FIXME enviar lista de peers
				DownloadThread.getInstance().updatePeers(null);
			} else {
				// FIXME enviar lista de peers
				DownloadThread.startInstance(null);
			}

			try {
				Thread.sleep(timing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
