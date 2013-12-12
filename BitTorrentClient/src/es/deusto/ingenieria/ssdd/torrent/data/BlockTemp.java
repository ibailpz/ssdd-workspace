package es.deusto.ingenieria.ssdd.torrent.data;

public class BlockTemp {

	private int pos;
	private byte[] bytes;
	private int miniBlocks = 0;

	public BlockTemp(int pos, int length) {
		this.pos = pos;
		bytes = new byte[length];
	}

	public synchronized void addBytes(byte[] b, int posi) {
		for (int i = posi, j = 0; j < b.length; i++, j++) {
			bytes[i] = b[j];
		}
		miniBlocks += b.length;
	}

	public boolean isFinished() {
		return miniBlocks == bytes.length;
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	public int getPos() {
		return pos;
	}

}
