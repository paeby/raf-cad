package sem2.nikolavulicevicTTT;


public class Message {
	int hash;
	Object obj;
	
	public Message(int hash, Object obj) {
		this.hash = hash;
		this.obj = obj;
	}
	

	public int getHash() {
		return hash;
	}
	public void setHash(int hash) {
		this.hash = hash;
	}

	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
}
