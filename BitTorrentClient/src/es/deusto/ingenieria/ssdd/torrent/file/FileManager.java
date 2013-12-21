package es.deusto.ingenieria.ssdd.torrent.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.swing.filechooser.FileSystemView;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class FileManager {

	private static FileManager instance;

	private MetainfoFile<?> metainfo;
	private FileObserver observer;
	private File data;
	private File tempData;

	/**
	 * Stores in each block position if that block is downloaded (1) or not (0)
	 */
	private int[] blocks;

	private int downloadedBlocks = 0;
	private Object _fileLock = new Object();

	private FileManager(MetainfoFile<?> metainfo) throws IOException {
		this.metainfo = metainfo;
		File parent = new File("downloads");
		if (!parent.exists()) {
			parent.mkdirs();
		}
		data = new File(parent, this.metainfo.getInfo().getName());
		tempData = new File(parent, this.metainfo.getInfo().getName()
				+ ".ttemp");
		this.blocks = new int[metainfo.getInfo().getHexStringSHA1().size()];
		for (int i = 0; i < this.blocks.length; i++) {
			this.blocks[i] = 0;
		}
		loadFile();
	}

	public static void initFileManager(MetainfoFile<?> meta) throws IOException {
		instance = new FileManager(meta);
	}

	public static FileManager getFileManager() {
		return instance;
	}

	public void setFileObserver(FileObserver fo) {
		this.observer = fo;
	}

	private void loadFile() throws IOException {
		// TODO Check in Linux
		System.out
				.println("File system roots returned byFileSystemView.getFileSystemView():");
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] roots = fsv.getRoots();
		for (int i = 0; i < roots.length; i++) {
			System.out.println("Root: " + roots[i]);
		}

		System.out.println("Home directory: " + fsv.getHomeDirectory());

		System.out.println("File system roots returned by File.listRoots():");
		File[] f = File.listRoots();
		for (int i = 0; i < f.length; i++) {
			System.out.println("Drive: " + f[i]);
			System.out.println("Display name: "
					+ fsv.getSystemDisplayName(f[i]));
			System.out.println("Is drive: " + fsv.isDrive(f[i]));
			System.out.println("Is floppy: " + fsv.isFloppyDrive(f[i]));
			System.out.println("Readable: " + f[i].canRead());
			System.out.println("Writable: " + f[i].canWrite());
			System.out.println("Total space: " + f[i].getTotalSpace());
			System.out.println("Usable space: " + f[i].getUsableSpace());
		}
		System.out.println();

		synchronized (_fileLock) {
			if (data.exists()) {
				downloadedBlocks = blocks.length;
				for (int i = 0; i < blocks.length; i++) {
					blocks[i] = 1;
				}
			} else {
				if (!tempData.exists()) {
					int length = this.metainfo.getInfo().getLength();
					length += (this.blocks.length * 4);
					System.out
							.println(length + " , " + tempData.getFreeSpace());
					// if (length >= data.getFreeSpace()) {
					// throw new IOException("Not enough space to create file");
					// }
					tempData.createNewFile();
					FileOutputStream fos = new FileOutputStream(tempData);
					byte[] b = new byte[1024];
					int t = b.length * (length / b.length);
					for (int i = 0; i < b.length; i++) {
						b[i] = -1;
					}
					for (int i = 0; i < t; i += b.length) {
						fos.write(b);
					}
					b = new byte[] { -1 };
					for (int i = t; i < length; i++) {
						fos.write(b);
					}
					fos.close();
				} else {
					FileInputStream fis = new FileInputStream(tempData);
					byte[] b = new byte[4];
					for (int i = 0; i < this.blocks.length; i++) {
						fis.read(b);
						int pos = ByteBuffer.wrap(b).getInt();
						if (pos > -1) {
							// TODO Check
							this.blocks[pos] = 1;
							downloadedBlocks++;
						} else {
							break;
						}
					}
					fis.close();
				}
			}
		}
	}

	/**
	 * Checks and saves a block to the temporal file
	 * 
	 * @param pos
	 *            Block position
	 * @param bytes
	 *            Block bytes
	 * @return If the file is completed or not
	 * @throws IOException
	 *             If an error occurs writing to the file
	 */
	public boolean checkAndSaveBlock(int pos, byte[] bytes) throws IOException {
		if (bytes[pos] != 1
				&& Arrays.equals(ToolKit.generateSHA1Hash(bytes), metainfo
						.getInfo().getByteSHA1().get(pos))) {
			synchronized (_fileLock) {
				FileOutputStream fos = new FileOutputStream(tempData);
				byte[] posArray = ByteBuffer.allocate(4).putInt(pos).array();
				fos.write(bytes, (blocks.length * 4) + getDownloadedSize(),
						bytes.length);
				fos.write(posArray, downloadedBlocks * 4, 4);
				fos.close();
				this.downloadedBlocks++;
				if (this.observer != null) {
					// this.observer.downloaded(bytes.length);
					this.observer.downloaded(1);
				}
			}
		}
		return isFinished();
	}

	public boolean isFinished() {
		return downloadedBlocks == blocks.length;
	}

	public int getDownloadedSize() {
		return this.metainfo.getInfo().getPieceLength() * this.downloadedBlocks;
	}

	public int getRemainingSize() {
		return this.metainfo.getInfo().getLength() - this.getDownloadedSize();
	}

	public int getTotalSize() {
		return this.metainfo.getInfo().getLength();
	}

	public int getTotalBlocks() {
		return this.blocks.length;
	}

	public int getDownloadedBlocks() {
		return downloadedBlocks;
	}

	public int getBlockLength() {
		return this.metainfo.getInfo().getPieceLength();
	}

	/**
	 * Checks the whole temp file and writes it to its final file format
	 * 
	 * @return true if the hash is correct and the file is copied successfully;
	 *         false otherwise
	 */
	public boolean checkAndWriteFile() {
		if (isFinished() && tempData.exists()) {
			observer.finishing();
			// TODO Check and write file
			// Read the initial array to know the saved position of each block

			boolean somethingBadHappened = false;
			if (somethingBadHappened) {
				observer.restart();
				return false;
			} else {
				observer.finished();
				return true;
			}
		}
		return false;
	}

}
