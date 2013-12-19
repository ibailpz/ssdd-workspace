package es.deusto.ingenieria.ssdd.torrent.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class FileManager {

	private static FileManager instance;

	private MetainfoFile<?> metainfo;
	private FileObserver observer;
	private File data;
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
		synchronized (_fileLock) {
			if (!data.exists()) {
				int length = this.metainfo.getInfo().getLength();
				length += (this.blocks.length * 4);
				System.out.println(length + " , " + data.getFreeSpace());
//				if (length >= data.getFreeSpace()) {
//					throw new IOException("Not enough space to create file");
//				}
				data.createNewFile();
				FileOutputStream fos = new FileOutputStream(data);
				byte[] b = new byte[] { -1 };
				for (int i = 0; i < length; i++) {
					fos.write(b);
				}
				fos.close();
			} else {
				FileInputStream fis = new FileInputStream(data);
				byte[] b = new byte[4];
				for (int i = 0; i < this.blocks.length; i++) {
					fis.read(b);
					int pos = ByteBuffer.wrap(b).getInt();
					if (pos > -1) {
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

	public void checkAndSaveBlock(int pos, byte[] bytes) throws IOException {
		if (Arrays.equals(ToolKit.generateSHA1Hash(bytes), metainfo.getInfo()
				.getByteSHA1().get(pos))) {
			synchronized (_fileLock) {
				FileOutputStream fos = new FileOutputStream(data);
				byte[] posArray = ByteBuffer.allocate(4).putInt(pos).array();
				fos.write(posArray, downloadedBlocks * 4, 4);
				fos.write(bytes, (blocks.length * 4) + getDownloadedSize(),
						bytes.length);
				fos.close();
				this.downloadedBlocks++;
				if (this.observer != null) {
					// this.observer.downloaded(bytes.length);
					this.observer.downloaded(1);
				}
			}

		}
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

}
