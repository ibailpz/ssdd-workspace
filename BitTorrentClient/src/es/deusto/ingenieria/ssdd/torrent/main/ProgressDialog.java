package es.deusto.ingenieria.ssdd.torrent.main;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import es.deusto.ingenieria.ssdd.torrent.file.FileObserver;

public class ProgressDialog extends JPanel implements FileObserver {

	private static final long serialVersionUID = 3129895159107929743L;

	private JProgressBar progress = new JProgressBar();
	private JPanel info = new JPanel();
	private JProgressBar indeterminate = new JProgressBar();

	public ProgressDialog(String name, int total, int current) {
		Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		progress.setMaximum(total);
		progress.setValue(current);
		progress.setStringPainted(true);
		BoxLayout b = new BoxLayout(this, BoxLayout.Y_AXIS);
		this.setLayout(b);
		JLabel label = new JLabel(name, SwingConstants.LEFT);
		label.setBorder(border);
		this.add(label);
		this.add(progress);
		indeterminate.setIndeterminate(true);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.add(indeterminate);
		this.add(info);
	}

	@Override
	public void downloaded(final int size) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				progress.setValue(progress.getValue() + size);
			}
		});
	}

	@Override
	public void finishing() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				WindowManager.getInstance().setWindowDefaultCloseOperation(
						JFrame.DO_NOTHING_ON_CLOSE);
				info.add(new JLabel(
						"Finishing download, don't close the window..."));
				WindowManager.getInstance().refresh();
			}
		});
	}

	@Override
	public void finished() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				WindowManager.getInstance().setWindowDefaultCloseOperation(
						JFrame.EXIT_ON_CLOSE);
				info.removeAll();
				info.add(new JLabel("Download finished!!"));
				WindowManager.getInstance().refresh();
			}
		});
	}

	@Override
	public void restart() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				WindowManager.getInstance().setWindowDefaultCloseOperation(
						JFrame.DISPOSE_ON_CLOSE);
				progress.setValue(0);
				info.removeAll();
				info.add(indeterminate);
				WindowManager.getInstance().refresh();
			}
		});
	}

}
