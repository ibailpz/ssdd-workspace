package es.deusto.ingenieria.ssdd.torrent.data;

import java.util.ArrayList;

public class BlockTemp {

	private final int pos;
	private final byte[] bytes;
	private final ArrayList<Integer> miniBlocks;
	private int bytesAdded = 0;
	private int nextMiniBlock = 0;

	public BlockTemp(int pos, int blockSize, int miniBlockSize) {
		this.pos = pos;
		bytes = new byte[blockSize];
		miniBlocks = new ArrayList<>();
		for (int i = 0; i < bytes.length; i += miniBlockSize) {
			miniBlocks.add(i);
		}
	}

	public synchronized void addBytes(byte[] b, int offset) {
		for (int i = offset, j = 0; i < bytes.length && j < b.length; i++, j++) {
			bytes[i] = b[j];
		}
		bytesAdded += b.length;
	}

	public boolean isFinished() {
		return bytesAdded >= bytes.length;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public int getPos() {
		return pos;
	}

	// public void miniBlockStarted(int length) {
	// nextMiniBlock += length;
	// }
	//
	// public void miniBlockDownloadFailed(int length) {
	// nextMiniBlock -= length;
	// }

	public void miniBlockDownloadFailed(int miniblock) {
		synchronized (miniBlocks) {
			miniBlocks.add(miniblock);
		}
	}

	public int getNextMiniBlock() {
		// return nextMiniBlock;
		synchronized (miniBlocks) {
			return miniBlocks.remove(0);
		}
	}

	public boolean hasMoreMiniBlocks() {
		return !miniBlocks.isEmpty();
	}

	public int size() {
		return bytes.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pos;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlockTemp other = (BlockTemp) obj;
		if (pos != other.pos)
			return false;
		return true;
	}

}
