package sem2.gorannikolicTTT.objects;

public class MyTimestamp implements Comparable<MyTimestamp> {

	private int timestamp;
	private int hash;
	
	public MyTimestamp(int timestamp, int hash) {
		super();
		this.timestamp = timestamp;
		this.hash = hash;
	}

	@Override
	public boolean equals(Object obj) {
		MyTimestamp temp = (MyTimestamp)obj;
		return timestamp == temp.timestamp && hash == temp.hash;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "(" +timestamp + "," + hash+  ")";
	}

	@Override
	public int compareTo(MyTimestamp o) {
		if(o.timestamp < timestamp)
			return 1;
		else if(o.timestamp > timestamp)
			return -1;
		return 0;
	}
}
