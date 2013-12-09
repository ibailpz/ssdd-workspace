package es.deusto.ingenieria.ssdd.torrent.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;

public class FileManager {

	private static FileManager instance;

	private MetainfoFile<?> metainfo;
	private File data;
	private int[] blocks;
	private int downloadedBlocks = 0;

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
		if (!data.exists()) {
			int length = this.metainfo.getInfo().getLength();
			length += (this.blocks.length * 4);
			if (length >= data.getFreeSpace()) {
				throw new IOException("Not enough space to create file");
			}
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

	public static void initFileManager(MetainfoFile<?> meta) throws IOException {
		instance = new FileManager(meta);
	}

	public static FileManager getFileManager() {
		return instance;
	}

	public void saveBlock(int pos, byte[] bytes) throws IOException {
		FileOutputStream fos = new FileOutputStream(data);
		byte[] posArray = ByteBuffer.allocate(4).putInt(pos).array();
		fos.write(posArray, downloadedBlocks * 4, 4);
		fos.write(bytes, (blocks.length * 4)
				+ (downloadedBlocks * metainfo.getInfo().getPieceLength()),
				bytes.length);
		fos.close();
	}

}
