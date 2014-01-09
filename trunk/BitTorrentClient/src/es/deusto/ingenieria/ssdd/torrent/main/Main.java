package es.deusto.ingenieria.ssdd.torrent.main;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class Main {

	private static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
		}
	}

	public static void main(String[] args) {
		setLookAndFeel();
		File file = WindowManager.getInstance().showChooseTorrentFile();
		if (file != null) {
			MetainfoFileHandler<?> handler;
			try {
				handler = new SingleFileHandler();
				handler.parseTorrenFile(file.getPath());
			} catch (Exception ex) {
				handler = new MultipleFileHandler();
				handler.parseTorrenFile(file.getPath());
			}
			MetainfoFile<?> meta = handler.getMetainfo();
			try {
				WindowManager.getInstance().showCreatingTempFiles();

				FileManager.initFileManager(meta);
				ProgressDialog progress = new ProgressDialog(meta.getInfo()
						.getName(), FileManager.getFileManager()
						.getTotalBlocks(), FileManager.getFileManager()
						.getDownloadedBlocks());

				WindowManager.getInstance().disposeTempFiles();

				FileManager.getFileManager().setFileObserver(progress);
				UploadThread.startInstance();
				TrackerThread.startTracker(meta);

				WindowManager.getInstance().showDownloading(
						meta.getInfo().getName(), progress);
			} catch (IOException e) {
				e.printStackTrace();
				// JOptionPane
				// .showMessageDialog(
				// null,
				// "Error creating file for download. Check that you have rights to write in the directory and that there is enough space for the file.",
				// "Error", JOptionPane.ERROR_MESSAGE);
				JOptionPane.showMessageDialog(null, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
