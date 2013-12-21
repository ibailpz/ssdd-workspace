package es.deusto.ingenieria.ssdd.torrent.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileFilter;

import es.deusto.ingenieria.ssdd.torrent.download.DownloadThread;
import es.deusto.ingenieria.ssdd.torrent.tracker.TrackerThread;
import es.deusto.ingenieria.ssdd.torrent.upload.UploadThread;

public class WindowManager {

	private static final WindowManager instance = new WindowManager();

	private JFrame root = new JFrame();
	private JProgressBar indet = new JProgressBar();

	private WindowManager() {
	}

	public static WindowManager getInstance() {
		return instance;
	}

	public File showChooseTorrentFile() {
		root.setSize(0, 0);
		root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		root.setUndecorated(true);
		root.setLocationRelativeTo(null);
		root.setVisible(true);

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
				if (f.isDirectory()) {
					return true;
				}
				String ext = null;
				String s = f.getName();
				int i = s.lastIndexOf('.');

				if (i > 0 && i < s.length() - 1) {
					ext = s.substring(i + 1).toLowerCase();
				}
				return "torrent".equals(ext);
			}
		});
		int dialog = jfc.showOpenDialog(root);
		root.dispose();
		if (dialog == JFileChooser.APPROVE_OPTION) {
			return jfc.getSelectedFile();
		} else {
			return null;
		}
	}

	public void showCreatingTempFiles() {
		root.setUndecorated(false);
		root.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		root.setTitle("Creating temp files");
		indet = new JProgressBar();
		indet.setIndeterminate(true);
		root.add(indet);
		root.pack();
		root.setResizable(false);
		root.setLocationRelativeTo(null);
		root.setVisible(true);
	}

	public void disposeTempFiles() {
		root.dispose();
		root.remove(indet);
	}

	public void showDownloading(String fileName, ProgressDialog progress) {
		root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		root.add(progress);
		root.setTitle("Downloading " + fileName + "...");
		root.pack();
		root.setResizable(false);
		root.setLocationRelativeTo(null);
		root.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				// FIXME Terminate threads on close
				TrackerThread.getInstance().interrupt();
				DownloadThread.getInstance().interrupt();
				UploadThread.getInstance().interrupt();
			}
		});
		root.setVisible(true);
	}

	public void setWindowDefaultCloseOperation(int op) {
		root.setDefaultCloseOperation(op);
	}

	public void refresh() {
		root.pack();
		root.setLocationRelativeTo(null);
	}

}
