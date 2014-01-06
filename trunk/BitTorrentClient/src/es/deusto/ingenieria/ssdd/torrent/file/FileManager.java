package es.deusto.ingenieria.ssdd.torrent.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

import es.deusto.ingenieria.ssdd.bitTorrent.metainfo.MetainfoFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class FileManager {

	private static FileManager instance;

	private MetainfoFile<?> metainfo;
	private FileObserver observer;
	private File tempCompleteFile;
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
		tempCompleteFile = new File(new File(
				System.getProperty("java.io.tmpdir"), "BitTorrent"),
				this.metainfo.getInfo().getName());
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
							// TODO Check
							this.blocks[pos] = 1;
							downloadedBlocks++;
						} else {
							break;
						}
					}
					System.out.println("FileManager - Current block status: "
							+ Arrays.toString(this.blocks));
					fis.close();
					System.out.println("FileManager - Data loaded");
				}
			}
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

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
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
				System.out
						.println("\tGenerated: " + Arrays.toString(blockHash));
				byte[] asciiHash = new String(blockHash).getBytes("ASCII");
				if (Arrays.equals(asciiHash, metainfo.getInfo().getByteSHA1()
						.get(pos))) {
					System.out
							.println("FileManager - Check correct. Saving...");

					RandomAccessFile raf = new RandomAccessFile(tempData, "rw");
					// FileOutputStream fos = new FileOutputStream(tempData);
					byte[] posArray = ByteBuffer.allocate(4).putInt(pos)
							.array();
					raf.seek((blocks.length * 4) + getDownloadedSize());
					raf.write(bytes);
					raf.seek(downloadedBlocks * 4);
					raf.write(posArray);
					// fos.write(bytes, (blocks.length * 4) +
					// getDownloadedSize(),
					// bytes.length);
					// fos.write(posArray, downloadedBlocks * 4, 4);
					blocks[pos] = 1;
					this.downloadedBlocks++;
					if (this.observer != null) {
						// this.observer.downloaded(bytes.length);
						this.observer.downloaded(1);
					}
					raf.close();
					// fos.close();
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

	public byte[] getBitfield() {
		byte[] bitfield = new byte[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			bitfield[i] = (byte) blocks[i];
		}
		// System.out.println("FileManager - Bitfield[" + bitfield.length +
		// "]: "
		// + Arrays.toString(bitfield));
		return bitfield;
	}

	public byte[] getBlock(int index) {
		if (blocks[index] < 1) {
			return null;
		}
		byte[] bytes = new byte[getBlockLength()];
		synchronized (_fileLock) {
			// FileInputStream fis = null;
			RandomAccessFile raf = null;
			try {
				if (isFinished()) {
					// fis = new FileInputStream(data);
					raf = new RandomAccessFile(data, "r");
					return loadBlock(index, bytes, raf, false);
				} else {
					// fis = new FileInputStream(tempData);
					raf = new RandomAccessFile(tempData, "r");
					return loadBlock(index, bytes, raf, true);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				bytes = null;
			} finally {
				// if (fis != null) {
				// try {
				// fis.close();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
				// }
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
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
	private byte[] loadBlock(int index, byte[] bytes, RandomAccessFile raf,
			boolean isTemp) throws IOException {
		raf.seek(0);
		if (isTemp) {
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
			raf.seek((index * getBlockLength()));
			int read = raf.read(bytes);
			return Arrays.copyOf(bytes, read);
		}
	}

	// private void loadBlock(int index, byte[] bytes, FileInputStream fis,
	// boolean isTemp) throws IOException {
	// if (isTemp) {
	// byte[] b = new byte[4];
	// int i = 0;
	// for (; i < this.blocks.length; i++) {
	// fis.read(b);
	// int pos = ByteBuffer.wrap(b).getInt();
	// if (pos == index) {
	// break;
	// }
	// }
	// if (i == this.blocks.length) {
	// throw new IOException("Block " + index
	// + " is not in the temp file");
	// }
	// fis.read(bytes, (this.blocks.length * 4) + (i * getBlockLength()),
	// getBlockLength());
	// } else {
	// fis.read(bytes, (index * getBlockLength()), getBlockLength());
	// }
	// }

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
			// TODO Check and write file
			// Read the initial array to know the saved position of each block
			tempCompleteFile.getParentFile().mkdirs();

			// FileInputStream tempDataInput = null;
			RandomAccessFile tempDataInput = null;
			FileOutputStream dataOutput = null;
			boolean completed = false;
			try {
				// tempDataInput = new FileInputStream(tempData);
				tempDataInput = new RandomAccessFile(tempData, "r");
				// int[] order = getBlocksStoredOrder(tempDataInput);
				dataOutput = new FileOutputStream(data);
				// storeData(order, tempDataInput, dataOutput);
				storeData(new int[0], tempDataInput, dataOutput);
			} catch (IOException e) {
				System.err.println("FileManager - " + e.getMessage());
				e.printStackTrace();
				return false;
			} finally {
				if (tempDataInput != null) {
					try {
						tempDataInput.close();
					} catch (IOException e) {
						System.err.println("FileManager - " + e.getMessage());
						e.printStackTrace();
					}
				}
				if (dataOutput != null) {
					try {
						dataOutput.close();
					} catch (IOException e) {
						System.err.println("FileManager - " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			completed = checkFinalFile();
			if (completed) {
				observer.finished();
				tempData.delete();
			} else {
				data.delete();
			}
			return completed;
		} else {
			System.out.println("FileManager - File is not complete yet");
			return false;
		}
	}

	private boolean checkFinalFile() {
		byte[] generatedHash = ToolKit.generateSHA1Hash(ToolKit
				.fileToByteArray(data.getAbsolutePath()));
		System.out.println("FileManager - Hash comparison:");
		System.out.println("\tOriginal:  "
				+ Arrays.toString(metainfo.getInfo().getInfoHash()));
		// try {
		// generatedHash = new String(generatedHash).getBytes("ASCII");
		// } catch (UnsupportedEncodingException e1) {
		// e1.printStackTrace();
		// }
		System.out.println("\tGenerated: " + Arrays.toString(generatedHash));
		if (Arrays.equals(metainfo.getInfo().getInfoHash(), generatedHash)) {
			System.out
					.println("FileManager - Hash check successful. Generating final file...");
			return true;
		} else {
			System.out
					.println("FileManager - Hash check failed. Starting over again...");
			try {
				initTemp();
			} catch (IOException e) {
				e.printStackTrace();
			}
			observer.restart();
			return false;
		}
	}

	private int[] getBlocksStoredOrder(FileInputStream tempDataInput)
			throws IOException {
		int[] order = new int[blocks.length];
		byte[] b = new byte[4];
		for (int i = 0; i < this.blocks.length; i++) {
			tempDataInput.read(b);
			int pos = ByteBuffer.wrap(b).getInt();
			order[i] = pos;
		}
		return order;
	}

	private void storeData(int[] order, RandomAccessFile raf,
			FileOutputStream dataOutput) throws IOException {
		// TODO Is really order needed??
		// int length = this.metainfo.getInfo().getLength();
		// fillFile(length, dataOutput);
		byte[] bytes;
		for (int i = 0; i < this.blocks.length; i++) {
			bytes = new byte[getBlockLength()];
			// loadBlock(order[i], bytes, tempDataInput);
			bytes = loadBlock(i, bytes, raf, true);
			dataOutput.write(bytes);
		}
	}

	// private void storeData(int[] order, FileInputStream tempDataInput,
	// FileOutputStream dataOutput) throws IOException {
	// // TODO Is really order needed??
	// // int length = this.metainfo.getInfo().getLength();
	// // fillFile(length, dataOutput);
	// byte[] bytes;
	// for (int i = 0; i < this.blocks.length; i++) {
	// bytes = new byte[getBlockLength()];
	// // loadBlock(order[i], bytes, tempDataInput);
	// loadBlock(order[i], bytes, tempDataInput, true);
	// dataOutput.write(bytes);
	// }
	// }

}
