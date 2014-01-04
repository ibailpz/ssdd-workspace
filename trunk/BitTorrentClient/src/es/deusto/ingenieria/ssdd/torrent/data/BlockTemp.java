package es.deusto.ingenieria.ssdd.torrent.data;

public class BlockTemp {

	private int pos;
	private byte[] bytes;
	private int miniBlocks = 0;
	private int nextMiniBlock = 0;

	public BlockTemp(int pos, int length) {
		this.pos = pos;
		bytes = new byte[length];
	}

	public synchronized void addBytes(byte[] b, int offset) {
		for (int i = offset, j = 0; i < bytes.length && j < b.length; i++, j++) {
			bytes[i] = b[j];
		}
		miniBlocks += b.length;
	}

	public int getStartOffset() {
		return miniBlocks;
	}

	public boolean isFinished() {
		return miniBlocks >= bytes.length;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public int getPos() {
		return pos;
	}

	public void miniBlockStarted(int length) {
		nextMiniBlock += length;
	}

	public void miniBlockDownloadFailed(int length) {
		nextMiniBlock -= length;
	}

	public int getNextMiniBlock() {
		return nextMiniBlock;
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
