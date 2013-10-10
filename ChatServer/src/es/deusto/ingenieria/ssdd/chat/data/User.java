package es.deusto.ingenieria.ssdd.chat.data;

public class User {
	private String ip;
	private int port;
	private String nick;
	private int state = 1;
	private int lastState = 1;

	public User(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(this.getClass())) {
			return this.nick.equalsIgnoreCase(((User) obj).nick);
		} else {
			return false;
		}
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		lastState = this.state;
		this.state = state;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public void goBackState() {
		this.state = lastState;
	}
}