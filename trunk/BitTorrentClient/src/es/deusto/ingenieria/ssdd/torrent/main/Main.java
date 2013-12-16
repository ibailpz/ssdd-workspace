package es.deusto.ingenieria.ssdd.torrent.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MetainfoFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.MultipleFileHandler;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.handler.SingleFileHandler;
import es.deusto.ingenieria.ssdd.torrent.file.FileManager;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class Main {

	public static void main(String[] args) {
		JFileChooser jfc = new JFileChooser();
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setMultiSelectionEnabled(false);
		jfc.addChoosableFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "Torrent files";
			}

			@Override
			public boolean accept(File f) {
				String ext = null;
				String s = f.getName();
				int i = s.lastIndexOf('.');

				if (i > 0 && i < s.length() - 1) {
					ext = s.substring(i + 1).toLowerCase();
				}
				return "torrent".equals(ext);
			}
		});
		int dialog = jfc.showOpenDialog(null);
		if (dialog == JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
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
				FileManager.initFileManager(meta);
				ProgressDialog progress = new ProgressDialog(meta.getInfo()
						.getName(), FileManager.getFileManager()
						.getTotalBlocks(), FileManager.getFileManager()
						.getDownloadedBlocks());
				FileManager.getFileManager().setFileObserver(progress);
				UploadThread.startInstance();
				TrackerThread.startTracker(meta);
				progress.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent arg0) {
						// FIXME Terminate threads on close
					}
				});
				progress.setVisible(true);
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane
						.showMessageDialog(
								null,
								"Error",
								"Error creating file for download. Check that you have rights to write in the directory and that there is enough space for the file.",
								JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
