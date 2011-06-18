package sem2.igorjovicTFT;

public class Pair {
	int key;
	Object value;
	
	public Pair(int k, Object v) {
		this.key = k;
		this.value = v;
	}
	
	@Override
	public String toString() {
		return "key:" + key + "\tvalue:" + value;
	}
	
}
