package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;


public class GetObjectConfirmed extends Message{
	private Object object;
	private int hash;
	public GetObjectConfirmed(MyTimestamp timestamp, Object obj, int hash) {
		super();
		this.timestamp = timestamp;
		object = obj;
		this.hash = hash;
	}

	@Override
	public void execute(MyDHT dht) {
		dht.receivedValues.put(hash, object);
		dht.pendingMessages.remove(timestamp);
	}

	@Override
	public String toString() {
		return "GetObjectConfirmed for: " + hash + "; timestamp:" + timestamp + "; object:" + object;
	}
}
