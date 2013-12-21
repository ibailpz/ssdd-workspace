package es.deusto.ingenieria.ssdd.torrent.data;

public class Peer {

	private String ip;
	private int port;

	public Peer(String ip, int port) {
		super();
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return ip + ":" + port;
	}

}
