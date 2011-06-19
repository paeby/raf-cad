package sem2.gorannikolicTTT.objects.messages;

import sem2.gorannikolicTTT.MyDHT;
import sem2.gorannikolicTTT.objects.MyTimestamp;



public abstract class Message {

	public MyTimestamp timestamp;
	public abstract void execute(MyDHT dht);
}
