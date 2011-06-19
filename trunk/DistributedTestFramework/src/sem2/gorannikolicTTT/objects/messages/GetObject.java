package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;


public class GetObject extends Message{

	private int requestor;
	private int hash;
	
	public GetObject(int requestor, int hash) {
		this.requestor = requestor;
		this.hash = hash;
	}
	
	@Override
	public void execute(MyDHT dht) {
		dht.system.sendMessage(requestor, 0, new GetObjectConfirmed(timestamp, dht.values.get(hash), hash));
	}

	@Override
	public String toString() {
		return "Get object for hash:" + hash + "; from:" + requestor + "; timestamp:" + timestamp;
	}
}
