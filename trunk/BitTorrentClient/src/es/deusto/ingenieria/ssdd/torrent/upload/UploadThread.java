package es.deusto.ingenieria.ssdd.torrent.upload;

public class UploadThread extends Thread {

	private static UploadThread instance;

	private UploadThread() {
	}

	public static void startInstance() {
		instance = new UploadThread();
		instance.start();
	}

	public static UploadThread getInstance() {
		return instance;
	}

	@Override
	public void run() {
		super.run();

	}
}
