package es.deusto.ingenieria.ssdd.chat.data;

public class User {	
	private String nick;
	private int state = 1;
	
	public String getNick() {
		return nick;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
	}
		
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(this.getClass())) {			
			return this.nick.equalsIgnoreCase(((User)obj).nick);				  
		} else {
			return false;
		}
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
}