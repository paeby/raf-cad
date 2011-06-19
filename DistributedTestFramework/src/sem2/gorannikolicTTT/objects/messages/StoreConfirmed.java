package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;


public class StoreConfirmed extends Message{
	public StoreConfirmed(MyTimestamp timestamp) {
		super();
		this.timestamp = timestamp;
	}

	@Override
	public void execute(MyDHT dht) {
		for(int i = dht.pendingMessages.size() - 1; i >= 0; i--)
			if(dht.pendingMessages.get(i) == timestamp)
				dht.pendingMessages.remove(i);
	}

	@Override
	public String toString() {
		return "Store confirmed:" + timestamp;
	}
}
