package sem2.bojannikitovicTFF;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class DHT implements DistributedHashTable, InitiableSolution
{
	private DistributedSystem system;

	private int index;
	private int nodesCount;

	private ConcurrentHashMap<Integer, Object> hashMap;

	@Override public void initialize()
	{
		hashMap = new ConcurrentHashMap<Integer, Object>();
		
		nodesCount = system.getProcessNeighbourhood().length + 1;
		
		// odredjuje index procesa
		index = -Arrays.binarySearch(system.getProcessNeighbourhood(), system.getProcessId()) - 1;
	}

	@Override public void messageReceived(int from, int type, Object msg)
	{
		Message message = (Message) msg;

		switch (type)
		{
			case GET:
				message.setValue(hashMap.get(message.getHashCode()));
				
				system.sendMessage(from, GET_RESP, message);
				
				break;
			case GET_RESP:
				getResp = message;
				
				break;
			case PUT:
				hashMap.put(message.getHashCode(), message.getValue());
				
				system.sendMessage(from, PUT_RESP, message);
				
				break;
			case PUT_RESP:
				putResp = message;
				
				break;
		}
	}

	@Override public void put(int hashCode, Object object)
	{
		int nodeIndex = hashCode % nodesCount;
		
		Message message = new Message(hashCode, object, system.getProcessId());

		if (nodeIndex != this.index)
		{
			if (nodeIndex >= this.index)
				nodeIndex -= 1;
			
			system.sendMessage(system.getProcessNeighbourhood()[nodeIndex], PUT, message);
			
			while (putResp == null)
				system.yield();
			
			putResp = null;
		}
		else
			hashMap.put(hashCode, object);
	}

	@Override public Object get(int hashCode)
	{
		int nodeIndex = hashCode % nodesCount;
		
		Message message = new Message(hashCode, null, system.getProcessId());

		if (nodeIndex != index)
		{
			if (nodeIndex >= index)
				nodeIndex -= 1;
			
			system.sendMessage(system.getProcessNeighbourhood()[nodeIndex], GET, message);
			
			while (getResp == null)
				system.yield();
			
			Object object = getResp.getValue();
			
			getResp = null;
			
			return object;
		}
		else
			return hashMap.get(hashCode);
	}

	public static void main(String[] args)
	{
		DistributedHashTableTester.testDHT(DHT.class, true, false, false);
	}
	
	private Message putResp;
	private Message getResp;

	private static final int PUT = 1;
	private static final int PUT_RESP = 2;
	private static final int GET = 3;
	private static final int GET_RESP = 4;
}
