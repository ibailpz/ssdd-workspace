package es.deusto.ingenieria.ssdd.torrent.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.FileDictionary;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MultipleFileInfoDictionary;
import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.SingleFileInfoDictionary;
import es.deusto.ingenieria.ssdd.bitTorrent.util.StringUtils;
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
		this.blocks = new int[metainfo.getInfo().getByteSHA1().size()];
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
		// System.out
		// .println("File system roots returned byFileSystemView.getFileSystemView():");
		// FileSystemView fsv = FileSystemView.getFileSystemView();
		// File[] roots = fsv.getRoots();
		// for (int i = 0; i < roots.length; i++) {
		// System.out.println("Root: " + roots[i]);
		// }
		//
		// System.out.println("Home directory: " + fsv.getHomeDirectory());
		//
		// System.out.println("File system roots returned by File.listRoots():");
		// File[] f = File.listRoots();
		// for (int i = 0; i < f.length; i++) {
		// System.out.println("Drive: " + f[i]);
		// System.out.println("Display name: "
		// + fsv.getSystemDisplayName(f[i]));
		// System.out.println("Is drive: " + fsv.isDrive(f[i]));
		// System.out.println("Is floppy: " + fsv.isFloppyDrive(f[i]));
		// System.out.println("Readable: " + f[i].canRead());
		// System.out.println("Writable: " + f[i].canWrite());
		// System.out.println("Total space: " + f[i].getTotalSpace());
		// System.out.println("Usable space: " + f[i].getUsableSpace());
		// }
		// System.out.println();

		System.out.println("FileManager - Loading data");

		synchronized (_fileLock) {
			if (data.exists()) {
				System.out.println("FileManager - Already downloaded");
				downloadedBlocks = blocks.length;
				for (int i = 0; i < blocks.length; i++) {
					blocks[i] = 1;
				}
			} else {
				if (!tempData.exists()) {
					System.out
							.println("FileManager - No temp data. Creating temp file...");
					// if (length >= data.getFreeSpace()) {
					// throw new IOException("Not enough space to create file");
					// }
					tempData.createNewFile();
					initTemp();
					System.out.println("FileManager - Temp file created");
				} else {
					System.out
							.println("FileManager - Temp data found. Loading contents...");
					FileInputStream fis = new FileInputStream(tempData);
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
					System.out.println("FileManager - Data loaded");
				}
			}
			System.out.println("FileManager - Current block status: "
					+ Arrays.toString(this.blocks));
		}
	}

	private void initTemp() throws IOException {
		int length = this.metainfo.getInfo().getLength();
		length += (this.blocks.length * 4);
		FileOutputStream fos = new FileOutputStream(tempData);
		fillFile(length, fos);
		fos.close();
	}

	private void fillFile(int length, FileOutputStream fos) throws IOException {
		byte[] b = new byte[1024];
		for (int i = 0; i < b.length; i++) {
			b[i] = -1;
		}

		int t = b.length * (length / b.length);
		for (int i = 0; i < t; i += b.length) {
			fos.write(b);
		}
		b = new byte[] { -1 };
		for (int i = t; i < length; i++) {
			fos.write(b);
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
		synchronized (_fileLock) {
			if (bytes[pos] != 1) {
				System.out.println("FileManager - Checking block " + pos);
				byte[] blockHash = ToolKit.generateSHA1Hash(bytes);
				System.out.println("FileManager - Hash comparison:");
				System.out.println("\tOriginal:  "
						+ Arrays.toString(metainfo.getInfo().getByteSHA1()
								.get(pos)));
				byte[] asciiHash = new String(blockHash).getBytes("ASCII");
				System.out
						.println("\tGenerated: " + Arrays.toString(asciiHash));
				System.out.println("\tOriginal:  "
						+ metainfo.getInfo().getHexStringSHA1().get(pos));
				System.out.println("\tGenerated: "
						+ StringUtils.toHexString(blockHash));
				if (Arrays.equals(asciiHash, metainfo.getInfo().getByteSHA1()
						.get(pos))) {
					// if (StringUtils.toHexString(blockHash).equals(
					// metainfo.getInfo().getHexStringSHA1().get(pos))) {
					System.out
							.println("FileManager - Check correct. Saving...");

					RandomAccessFile raf = new RandomAccessFile(tempData, "rw");
					byte[] posArray = ByteBuffer.allocate(4).putInt(pos)
							.array();
					raf.seek((blocks.length * 4) + getDownloadedSize());
					raf.write(bytes);
					raf.seek(downloadedBlocks * 4);
					raf.write(posArray);
					blocks[pos] = 1;
					this.downloadedBlocks++;
					if (this.observer != null) {
						this.observer.downloaded(1);
					}
					raf.close();
					System.out.println("FileManager - Block " + pos
							+ " correctly stored");
				} else {
					System.out.println("FileManager - Wrong hash");
				}
			} else {
				System.out.println("FileManager - Block " + pos
						+ " already downloaded");
			}
		}
		return isFinished();
	}

	public boolean isFinished() {
		return downloadedBlocks == blocks.length;
	}

	public int getDownloadedSize() {
		if (this.blocks[this.blocks.length - 1] > 0) {
			return (this.metainfo.getInfo().getPieceLength() * (this.downloadedBlocks - 1))
					+ this.getBlockSize(this.blocks.length - 1);
		} else {
			return this.metainfo.getInfo().getPieceLength()
					* this.downloadedBlocks;
		}
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

	public byte[] getBitfield() {
		byte[] bitfield = new byte[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			bitfield[i] = (byte) (blocks[i] < 1 ? 0 : 1);
		}
		return bitfield;
	}

	public byte[] getBlock(int index) {
		if (blocks[index] < 1) {
			return null;
		}
		byte[] bytes = new byte[getBlockLength()];
		synchronized (_fileLock) {
			try {
				return loadBlock(index, bytes, !isFinished());
			} catch (IOException ex) {
				ex.printStackTrace();
				bytes = null;
			}
		}
		return bytes;
	}

	/**
	 * Loads the specified block of data into the byte array
	 * 
	 * @param index
	 *            The block index
	 * @param bytes
	 *            The array to store the block data
	 * @param fis
	 *            The FileInputStream to read from
	 * @param isTemp
	 *            true if the FileInputStream makes reference to a temp data
	 *            file; false if it makes reference to the original data file
	 * @throws IOException
	 *             If any error happens when accessing the file
	 */
	private byte[] loadBlock(int index, byte[] bytes, boolean isTemp)
			throws IOException {
		RandomAccessFile raf = null;
		try {
			if (isTemp) {
				raf = new RandomAccessFile(tempData, "r");
				raf.seek(0);
				byte[] b = new byte[4];
				int i = 0;
				for (; i < this.blocks.length; i++) {
					raf.read(b);
					int pos = ByteBuffer.wrap(b).getInt();
					if (pos == index) {
						break;
					}
				}
				if (i == this.blocks.length) {
					throw new IOException("Block " + index
							+ " is not in the temp file");
				}
				raf.seek((this.blocks.length * 4) + (i * getBlockLength()));
				int read = raf.read(bytes);
				return Arrays.copyOf(bytes, read);
			} else {
				if (metainfo.getInfo() instanceof SingleFileInfoDictionary) {
					raf = new RandomAccessFile(data, "r");
					raf.seek(index * getBlockLength());
					int read = raf.read(bytes);
					return Arrays.copyOf(bytes, read);
				} else {
					MultipleFileInfoDictionary info = (MultipleFileInfoDictionary) metainfo
							.getInfo();
					List<FileDictionary> files = info.getFiles();
					int bytePos = index * getBlockLength();
					int fileIndex = 0;
					boolean found = false;
					int fileBytes = 0;
					do {
						fileBytes += files.get(fileIndex).getLength();
						if (fileBytes > bytePos) {
							found = true;
						} else {
							fileIndex++;
						}
					} while (!found);
					int read = 0;
					do {
						raf = new RandomAccessFile(new File(data.getParent(),
								files.get(fileIndex).getPath()), "r");
						read += raf.read(bytes, read, bytes.length - read);
						fileIndex++;
					} while (read < bytes.length && fileIndex < files.size());
					if (read < bytes.length) {
						return null;
					} else {
						return bytes;
					}
				}
			}
		} finally {
			if (raf != null) {
				raf.close();
			}
		}
	}

	public byte[] getInfoHash() {
		return metainfo.getInfo().getInfoHash();
	}

	public int getNextPosToDownload(int start) {
		int pos = -1;
		for (int i = start; i < blocks.length; i++) {
			if (blocks[i] < 1) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	public int getBlockSize(int block) {
		if (block == (blocks.length - 1)) {
			return getTotalSize() - (getBlockLength() * (blocks.length - 1));
		} else {
			return getBlockLength();
		}
	}

	/**
	 * Checks the whole temp file and writes it to its final file format
	 * 
	 * @return true if the hash is correct and the file is copied successfully;
	 *         false otherwise
	 */
	public boolean checkAndWriteFile() {
		System.out.println("FileManager - Checking temp file...");
		if (isFinished() && tempData.exists()) {
			observer.finishing();
			try {
				storeData();
			} catch (IOException e) {
				System.err.println("FileManager - " + e.getMessage());
				e.printStackTrace();
				deleteFileOrDirectory(data);
				return false;
			}

			observer.finished();
			tempData.delete();
			return true;
		} else {
			System.out.println("FileManager - File is not complete yet");
			return false;
		}
	}

	private void deleteFileOrDirectory(File file) {
		if (file.isDirectory()) {
			File[] listFiles = file.listFiles();
			for (File f : listFiles) {
				deleteFileOrDirectory(f);
			}
		}
		file.delete();
	}

	private void storeData() throws IOException {
		RandomAccessFile tempDataInput = new RandomAccessFile(tempData, "r");
		FileOutputStream dataOutput = null;
		try {
			if (metainfo.getInfo() instanceof SingleFileInfoDictionary) {
				dataOutput = new FileOutputStream(data);
				byte[] bytes;
				for (int i = 0; i < this.blocks.length; i++) {
					bytes = new byte[getBlockLength()];
					bytes = loadBlock(i, bytes, true);
					dataOutput.write(bytes);
				}
			} else {
				MultipleFileInfoDictionary info = (MultipleFileInfoDictionary) metainfo
						.getInfo();
				List<FileDictionary> files = info.getFiles();
				byte[] bytes = null;
				int fileIndex = 0;
				int blockIndex = 0;
				int wrote = 0;
				boolean read = true;
				File current = new File(data.getParent(), files.get(fileIndex)
						.getPath());
				current.getParentFile().mkdirs();
				dataOutput = new FileOutputStream(current);
				do {
					if (read) {
						bytes = new byte[getBlockLength()];
						bytes = loadBlock(blockIndex, bytes, true);
					} else {
						read = true;
					}
					int left = files.get(fileIndex).getLength() - wrote;
					if (bytes.length > left) {
						dataOutput.write(bytes, 0, left);
						dataOutput.close();
						fileIndex++;
						if (fileIndex != files.size()) {
							current = new File(data.getParent(), info
									.getFiles().get(fileIndex).getPath());
							current.getParentFile().mkdirs();
							dataOutput = new FileOutputStream(current);
							bytes = Arrays.copyOfRange(bytes, left,
									bytes.length);
							read = false;
							wrote = 0;
						}
					} else {
						dataOutput.write(bytes);
						wrote += bytes.length;
						blockIndex++;
					}
				} while (fileIndex < files.size() && blockIndex < blocks.length);
			}
		} finally {
			if (dataOutput != null) {
				dataOutput.close();
			}
			if (tempDataInput != null) {
				tempDataInput.close();
			}
		}
	}
}
