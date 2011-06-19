package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;


public class StoreObject extends Message{

	public boolean firstStore = true;
	private Object objToStore;
	private int requestor;
	private int hash;
	
	public static int TIMEOUT;
	
	public StoreObject(boolean firstStore, Object objToStore, int requestor,
			int hash, MyTimestamp timestamp) {
		this.firstStore = firstStore;
		this.objToStore = objToStore;
		this.requestor = requestor;
		this.hash = hash;
		this.timestamp = timestamp;
	}
	
	public StoreObject(boolean firstStore, Object objToStore, int requestor,
			int hash) {
		this.firstStore = firstStore;
		this.objToStore = objToStore;
		this.requestor = requestor;
		this.hash = hash;
	}
	

	@Override
	public void execute(MyDHT dht) {
		if(firstStore)
		{
			dht.values.put(hash, objToStore);
			StoreObject str = new StoreObject(false, objToStore, dht.system.getProcessId(), hash);
			
			dht.sendMsgAndWait(hash, str, (dht.system.getProcessId() ^ 0x01), dht.pendingMessages);
			
			dht.system.sendMessage(requestor, 0, new StoreConfirmed(timestamp));
		}
		else
		{
			dht.values.put(hash, objToStore);
			if(requestor != dht.system.getProcessId())
				dht.system.sendMessage(requestor, 0, new StoreConfirmed(timestamp));
			else
				dht.messageReceived(0, 0, new StoreConfirmed(timestamp));
		}
	}

	@Override
	public String toString() {
		String s = "From:" + requestor + "; firstStore:" + firstStore + "; hash:" + hash + "; timestamp:" + timestamp;
		return s;
	}
}
