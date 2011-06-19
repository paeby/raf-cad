package sem2.gorannikolicTTT;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import sem2.gorannikolicTTT.objects.MyTimestamp;
import sem2.gorannikolicTTT.objects.messages.GetKClosest;

public class Buckets {

	public List<ArrayList<Integer>> buckets = new ArrayList<ArrayList<Integer>>();
	
	public List<MyTimestamp> pendingQueries = new ArrayList<MyTimestamp>();
	
	public int[] neighbors;
	
	public static List<Integer> allneighbors = new ArrayList<Integer>();

	public int myId;

	public static long TIMEOUT;
	
	public TreeMap<MyTimestamp, Integer> returnedClosest = new TreeMap<MyTimestamp, Integer>();
	public Buckets(int[] neighbors, int myId) {
		this.neighbors = neighbors;
		this.myId = myId;
		
		for (int i = 0; i < 8; i++) {
			buckets.add(new ArrayList<Integer>());
		}
		
		for (int i : neighbors) {
			buckets.get(7 - distanceBetween(myId, i)).add(i);
		}
	}

	private int distanceBetween(int i, int j) {
		int a = i ^ j;
		int distance = 0;
		while(a < 128)
		{
			distance++;
			a <<= 1;
		}
		return distance;
	}

	public List<Integer> getKClosestOne(MyDHT dht, int hash) {

		List<Integer> tempList = new ArrayList<Integer>();

		int minId = findClosest(dht, hash);
		if((minId ^ hash) < ((minId ^ 0x01) ^ hash))
		{
			tempList.add(minId);
			tempList.add(minId ^ 0x01);
		}
		else
		{
			tempList.add(minId ^ 0x01);
			tempList.add(minId);
		}
		
		return tempList;
	}
	
	private int findClosest(MyDHT dht, int hash)
	{
		MyTimestamp timestamp = new MyTimestamp(++dht.msgsSent - 1, hash);
		
		int tempClosest = getMyClosest(hash, false);
		int prevTemp = 0;
		GetKClosest msg = new GetKClosest(timestamp, hash, myId, tempClosest);
		if(myId != tempClosest)
			dht.system.sendMessage(tempClosest, 0, msg);
		else
			return myId;
		long timeSent = System.currentTimeMillis();
		
		pendingQueries.add(timestamp);
		while(true)
		{
			dht.system.yield();
			if(System.currentTimeMillis() - timeSent > TIMEOUT)
			{
				pendingQueries.remove(timestamp);
				
				if(prevTemp == (tempClosest ^ 0x01))
					return tempClosest;
				
				prevTemp = tempClosest;
				
//				tempClosest = getMyClosest(hash, true);
				tempClosest = tempClosest ^ 0x01;
				
				timestamp = new MyTimestamp(++dht.msgsSent - 1, hash);

				msg = new GetKClosest(timestamp, hash, myId, tempClosest);
				if(tempClosest != myId)
					dht.system.sendMessage(tempClosest, 0, msg);
				else
					return myId;
				timeSent = System.currentTimeMillis();
				
				pendingQueries.add(timestamp);
			}
			else if(!pendingQueries.contains(timestamp))
			{
				if((tempClosest ^ hash) > (returnedClosest.get(timestamp) ^ hash))
				{
					prevTemp = tempClosest;
					tempClosest = returnedClosest.get(timestamp);
					timestamp = new MyTimestamp(++dht.msgsSent - 1, hash);
//					returnedClosest.clear();
					
					msg = new GetKClosest(timestamp, hash, myId, tempClosest);
					if(tempClosest != myId)
						dht.system.sendMessage(tempClosest, 0, msg);
					else
						return myId;
					timeSent = System.currentTimeMillis();
					
					pendingQueries.add(timestamp);
				}
				else
					return returnedClosest.get(timestamp);
			}
		}
	}
	
	public int getMyClosest(int hash, boolean second)
	{
		int min = (myId ^ hash);
		int minId = myId;
		
		for (int i = 0; i < neighbors.length; i++) {
			if(min > (neighbors[i] ^ hash))
			{
				minId = neighbors[i];
				min = (neighbors[i] ^ hash);
			}
		}

		if(second)
		{
			int firstMinId = minId;
			min = 300;
			minId = 0;
			for (int i = 0; i < neighbors.length; i++) {
				if(min > (neighbors[i] ^ hash) && neighbors[i] != firstMinId)
				{
					minId = neighbors[i];
					min = (neighbors[i] ^ hash);
				}
			}
			
			if(min > (myId ^ hash) && myId != firstMinId)
				minId = myId;
		}
		
		return minId;
	}
	
	public List<Integer> getKClosestEasy(int hash)
	{
		if(allneighbors.size() < 16)
			return new ArrayList<Integer>();
		List<Integer> tempList = new ArrayList<Integer>();
		
		int min = 300;
		int minId = 0;
		
		for (int i = 0; i < allneighbors.size(); i++) {
			if(min > (hash ^ allneighbors.get(i)))
			{
				minId = allneighbors.get(i);
				min = (hash ^ allneighbors.get(i));
			}
		}
		
		if((minId ^ hash) < ((minId ^ 0x01) ^ hash))
		{
			tempList.add(minId);
			tempList.add(minId ^ 0x01);
		}
		else
		{
			tempList.add(minId ^ 0x01);
			tempList.add(minId);
		}
		return tempList;
	}
	
}
