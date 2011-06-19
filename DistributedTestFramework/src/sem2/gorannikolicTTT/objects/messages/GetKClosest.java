package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;

public class GetKClosest extends Message{
	public int hash;
	
	public int requester;
	public int receiver;
	
	public GetKClosest(MyTimestamp timestamp, int hash,
			int requester, int receiver) {
		super();
		this.timestamp = timestamp;
		this.hash = hash;
		this.requester = requester;
		this.receiver = receiver;
	}

	@Override
	public void execute(MyDHT dht) {
		KClosestReturned returned = new KClosestReturned(dht.buckets.getMyClosest(hash, false), timestamp, hash);
		if(receiver != requester)
			dht.system.sendMessage(requester, 0, returned);
		else
			dht.messageReceived(0, 0, returned);
	}

	@Override
	public String toString() {
		String s = "GetKClosest: requester:" + requester + "; receiver:" + receiver + "hash:" + hash + "; timestamp:" + timestamp;
		return s;
	}
}
