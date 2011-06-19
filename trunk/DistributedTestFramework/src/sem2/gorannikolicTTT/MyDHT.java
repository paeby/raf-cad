package sem2.gorannikolicTTT;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;
import sem2.gorannikolicTTT.objects.MyTimestamp;
import sem2.gorannikolicTTT.objects.messages.GetObject;
import sem2.gorannikolicTTT.objects.messages.Message;
import sem2.gorannikolicTTT.objects.messages.StoreObject;

public class MyDHT implements DistributedHashTable, InitiableSolution {

	public Buckets buckets;
	public DistributedSystem system;
	
	public int msgsSent = 0;
	
	public TreeMap<Integer, Object> values = new TreeMap<Integer, Object>();
	
	public List<MyTimestamp> pendingMessages = new ArrayList<MyTimestamp>();
	
	public TreeMap<Integer, Object> receivedValues = new TreeMap<Integer, Object>();
	
	public static long TIMEOUT;
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		initialize();
		((Message)message).execute(this);
	}

	@Override
	public void initialize() {
		if(buckets == null)
		{
			buckets = new Buckets(system.getProcessNeighbourhood(), system.getProcessId());
			Buckets.allneighbors.add(system.getProcessId());
		}
		
		StoreObject.TIMEOUT = 50;	// for waiting backup process to respond
		MyDHT.TIMEOUT = 50; 		// for waiting store or get proces to respond
		Buckets.TIMEOUT = 100;		// for waiting process to return his closest ids
	}

	@Override
	public void put(int hash, Object object) {
		List<Integer> tempKClosest = buckets.getKClosestEasy(hash);
		List<Integer> kClosest = buckets.getKClosestOne(this, hash);
//		if(tempKClosest.size() != 0)
//		System.out.println(system.getProcessId() + " " + "Get Hash:" + hash + "; kClosest:" + Arrays.toString(kClosest.toArray()) +
//				"; real kClosest:" + Arrays.toString(tempKClosest.toArray()) + "==" + (kClosest.get(0).intValue() == tempKClosest.get(0).intValue() && kClosest.get(1).intValue() == tempKClosest.get(1).intValue()));
			
//		System.out.println("\t"+system.getProcessId() + " " + Arrays.toString(system.getProcessNeighbourhood()));
		StoreObject str = new StoreObject(true, object, system.getProcessId(), hash);

		if(amIInList(kClosest) == -1)
		{
			if(sendMsgAndWait(hash, str, kClosest.get(0), pendingMessages))
				return;
			else
			{
				str.firstStore = false;
				sendMsgAndWait(hash, str, kClosest.get(1), pendingMessages);
				return;
			}
		}
		else
		{
			values.put(hash, object);
			str.firstStore = false;
			sendMsgAndWait(hash, str, kClosest.get(amIInList(kClosest)), pendingMessages);
			return;
		}
	}

	private int amIInList(List<Integer> kClosest) {
		if(kClosest.get(0) == system.getProcessId())
			return 1;
		if(kClosest.get(1) == system.getProcessId())
			return 0;
		return -1;
	}

	@Override
	public Object get(int hash) {
		List<Integer> tempKClosest = buckets.getKClosestEasy(hash);
		List<Integer> kClosest = buckets.getKClosestOne(this, hash);
//		if(tempKClosest.size() != 0)
//		System.out.println(system.getProcessId() + " " + "Get Hash:" + hash + "; kClosest:" + Arrays.toString(kClosest.toArray()) +
//				"; real kClosest:" + Arrays.toString(tempKClosest.toArray()) + "==" + (kClosest.get(0).intValue() == tempKClosest.get(0).intValue() && kClosest.get(1).intValue() == tempKClosest.get(1).intValue()));
		
		//		System.out.println("\t"+system.getProcessId() + " " + Arrays.toString(system.getProcessNeighbourhood()));
		GetObject str = new GetObject(system.getProcessId(), hash);

		if(amIInList(kClosest) == -1)
		{
			
			if(sendMsgAndWait(hash, str, kClosest.get(0), pendingMessages))
				return receivedValues.remove(hash);
			else
			{
				sendMsgAndWait(hash, str, kClosest.get(1), pendingMessages);
				return receivedValues.remove(hash);
			}
		}
		if(values.get(hash) == null)
		{
			sendMsgAndWait(hash, str, kClosest.get(amIInList(kClosest)), pendingMessages);
			return receivedValues.remove(hash);
		}
		return values.get(hash);
	}
	
	public boolean sendMsgAndWait(int hash, Message msg, int to, List<MyTimestamp> pendingList)
	{
		MyTimestamp timestamp = new MyTimestamp(++msgsSent - 1, hash);
		pendingList.add(timestamp);
		msg.timestamp = timestamp;
		
		system.sendMessage(to, 0, msg);
		long timeSent = System.currentTimeMillis();
		
		while(true)
		{
			system.yield();
			if(System.currentTimeMillis() - timeSent > TIMEOUT)
			{
				pendingMessages.remove(timestamp);
				return false;
			}
			else if(!pendingList.contains(timestamp))
				return true;
		}
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(MyDHT.class, true, true, true);
	}
}
