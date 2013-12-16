package es.deusto.ingenieria.ssdd.torrent.main;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import es.deusto.ingenieria.ssdd.torrent.file.FileObserver;

public class ProgressDialog extends JFrame implements FileObserver {

	private static final long serialVersionUID = 3129895159107929743L;
	
	JProgressBar progress = new JProgressBar();

	public ProgressDialog(String name, int total, int current) {
		Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		progress.setMaximum(total);
		progress.setValue(current);
		progress.setStringPainted(true);
		BoxLayout b = new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS);
		this.setLayout(b);
		JLabel label = new JLabel(name, SwingConstants.LEFT);
		label.setBorder(border);
		this.add(label);
		progress.setBorder(border);
		this.add(progress);
		JProgressBar in = new JProgressBar();
		in.setIndeterminate(true);
		in.setBorder(border);
		this.add(in);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Downloading " + name + "...");
		this.pack();
		this.setMinimumSize(new Dimension(300, 100));
		this.setResizable(false);
		this.setLocationRelativeTo(null);
	}

	@Override
	public void downloaded(int size) {
		progress.setValue(progress.getValue() + size);
	}

}
