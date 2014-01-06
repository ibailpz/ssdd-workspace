package es.deusto.ingenieria.ssdd.torrent.data;

public class Peer {

	private String ip;
	private int port;
	private int[] bitfield;
	private boolean chockedUs = false;

	public Peer(String ip, int port, int bitfieldSize) {
		this.ip = ip;
		this.port = port;
		this.bitfield = new int[bitfieldSize];
		for (int i = 0; i < bitfieldSize; i++) {
			bitfield[i] = -1;
		}
		System.out.println("Peer created " + this.toString());
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public int[] getBitfield() {
		return bitfield;
	}

	public synchronized void setBitfield(int[] bitfield) {
		// this.bitfield = bitfield;
		if (this.bitfield.length != bitfield.length) {
			System.out.println("Different length bitfield received. Weird...");
			return;
		}
		for (int i = 0; i < bitfield.length; i++) {
			this.bitfield[i] = Math.max(this.bitfield[i], bitfield[i]);
		}
	}

	public synchronized void setBitfieldPosition(int position, int state) {
		bitfield[position] = state;
	}

	public boolean isChockedUs() {
		return chockedUs;
	}

	public void setChockedUs(boolean chockedUs) {
		this.chockedUs = chockedUs;
	}

	@Override
	public String toString() {
		return ip + ":" + port;
	}

}
