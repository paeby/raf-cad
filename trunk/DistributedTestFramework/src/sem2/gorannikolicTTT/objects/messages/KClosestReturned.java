package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;





public class KClosestReturned extends Message{
	public int kClosest;
	public int hash;
	
	public KClosestReturned(int kClosest, MyTimestamp timestamp, int hash) {
		super();
		
		this.timestamp = timestamp;
		this.hash = hash;
	
		this.kClosest = kClosest;
	}

	@Override
	public void execute(MyDHT dht) {
		if(dht.buckets.pendingQueries.contains(timestamp))
		{
			dht.buckets.returnedClosest.put(timestamp, kClosest);
			dht.buckets.pendingQueries.remove(timestamp);
		}
	}
	
	@Override
	public String toString() {
		String s = "KClosestReturned: hash:" + hash + "; timestamp:" + timestamp + "; kClosest:" + kClosest;
		return s;
	}

}
