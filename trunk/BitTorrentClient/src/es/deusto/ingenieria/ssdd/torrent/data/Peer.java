package es.deusto.ingenieria.ssdd.torrent.data;

public class Peer {

	private String ip;
	private int port;
	private int[] bitfield;
	private boolean chockedUs = false;

	public Peer(String ip, int port, int bitfieldSize) {
		super();
		this.ip = ip;
		this.port = port;
		this.bitfield = new int[bitfieldSize];
		for (int i = 0; i < bitfieldSize; i++) {
			bitfield[i] = -1;
		}
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

	public void setBitfield(int[] bitfield) {
		this.bitfield = bitfield;
	}

	public void setBitfieldPosition(int position, int state) {
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
