package sem2.kojaTTT;

public class Element {
	private int hash;
	private Object object;
	
	public Element(int hash, Object object) {
		super();
		this.hash = hash;
		this.object = object;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	
	
}
