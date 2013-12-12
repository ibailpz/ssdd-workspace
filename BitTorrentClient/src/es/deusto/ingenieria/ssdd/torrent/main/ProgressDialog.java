package es.deusto.ingenieria.ssdd.torrent.main;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

import es.deusto.ingenieria.ssdd.torrent.file.FileObserver;

public class ProgressDialog extends JFrame implements FileObserver {

	JProgressBar progress = new JProgressBar();

	public ProgressDialog(int total, int current) {
		progress.setMaximum(total);
		progress.setValue(current);
		progress.setStringPainted(true);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(progress);
		JProgressBar in = new JProgressBar();
		in.setIndeterminate(true);
		this.add(in);
		this.pack();
		this.setLocationRelativeTo(null);
	}

	@Override
	public void blockDownloaded() {
		progress.setValue(progress.getValue() + 1);
	}

}
