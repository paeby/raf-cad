// Cvetkovic Milan RN 2909

package sem2;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

import kids.dist.common.DistributedSystem;
import kids.dist.seminarski2.DistributedHashTableTester;


public class CvetkovicMilanTTT implements kids.dist.seminarski2.DistributedHashTable, kids.dist.common.problem.InitiableSolution{

	private DistributedSystem system;
	private Hashtable<Integer, Object> values = new Hashtable<Integer, Object>();
	private Object value = null;
	private boolean confirmed = false;
	private int[] ids = null;
	private int carrier = -1; 
	private boolean found = false;
	private long wait = 50;
	private LinkedList<Integer> cfmdIds = new LinkedList<Integer>();
	private int cfmId = 0;
	
	private int[] intToBits(int number, int numberOfBits) {
		int[] n = new int[numberOfBits];
		for (int i = 0; i < n.length; i++) n[i] = 0;
		
		char[] c = (Integer.toBinaryString(number)).toCharArray();
		for (int i = n.length-1, j = c.length - 1; j >= 0; i--, j--) n[i] = Character.getNumericValue(c[j]);
		
		return n;
	}
	
	private int bitsToInt(int[] bits) {
		int number = 0;
		for (int i = bits.length-1, j = 0; i >= 0; i--, j++)
			if(bits[i] == 1) number += Math.pow(2, j);
		
		return number;
	}
	
	private int closestValue(int number, int[] array){
		int[] n = intToBits(number, 8);
		
		int longestMatch = 0;
		int closest = -1;
		for (int i = 0; i < array.length; i++) {
			int[] id = intToBits(array[i], 8);
			int len = 0;
			
			for (int j = 0; j < n.length; j++) 
				if(n[j] == id[j]) len++;
				else
				{
					if(len >= longestMatch)
					{
						longestMatch = len;
						closest = array[i];
					}
					break;
				}
		}
		
		return closest;
	}
	
	private int backupCarrier(int carrier) {
		int[] bc = intToBits(carrier, 8); 
		
		if(bc[bc.length-1] == 0) bc[bc.length-1] = 1;
		else bc[bc.length-1] = 0;
		
		return bitsToInt(bc);
	}
	
	@Override
	public void initialize() {
		ids = new int[system.getProcessNeighbourhood().length + 1];
		
		for (int i = 0; i < system.getProcessNeighbourhood().length; i++) 
			ids[i] = system.getProcessNeighbourhood()[i];
		
		ids[ids.length - 1] = system.getProcessId();
		
		Arrays.sort(ids);

		//System.out.println(system.getProcessId()+" : "+Arrays.toString(system.getProcessNeighbourhood()));
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		switch (type) {
		case 0:
			confirmed = true;
			value = message;
			break;
		case 1:
			values.put(((HashMessage)message).key, ((HashMessage)message).value);
			system.sendMessage(from, 0, null);
			break;
		case 2:
			system.sendMessage(from, 0, values.get((Integer)message));
			break;
		case 3: 
			if(from == ((FindNodeMessage)message).id) system.sendMessage(from, 0, -1);
			else system.sendMessage(from, 5, ((FindNodeMessage)message).cfmdId);
			
			int cId = cfmId++;
			
			int closest = closestValue(((FindNodeMessage)message).hash, ids);
			if (closest == system.getProcessId()) system.sendMessage(((FindNodeMessage)message).id, 4, closest); 
			else
			{
				system.sendMessage(closest, 3, new FindNodeMessage(((FindNodeMessage)message).hash, ((FindNodeMessage)message).id, cId));
				
				long time = System.currentTimeMillis();
				while(!cfmdIds.contains(cId) && time + wait >= System.currentTimeMillis()) system.yield(); 
				
				if(!cfmdIds.contains(cId))
				{
					closest = backupCarrier(closest);
					if(closest == system.getProcessId()) system.sendMessage(((FindNodeMessage)message).id, 4, closest); 
					else 
					{
						system.sendMessage(closest, 3, new FindNodeMessage(((FindNodeMessage)message).hash, ((FindNodeMessage)message).id, cId));
						while(!cfmdIds.contains(cId)) system.yield();
					}
				}
				
				cfmdIds.remove((Object)cId);
			}			
			break;
		case 4:
			found = true;
			carrier = (Integer)message;
			break;
		case 5:
			cfmdIds.addLast((Integer)message);
			break;
		}
	}

	@Override
	public void put(int hash, Object object) {

		int closest = closestValue(hash, ids);
		
		if(closest == system.getProcessId())
		{
			found = true;
			carrier = closest;
		}
		else
		{
			system.sendMessage(closest, 3, new FindNodeMessage(hash, system.getProcessId(), -1));
			
			long time = System.currentTimeMillis();
			while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
			
			if(!confirmed)
			{
				closest = backupCarrier(closest);
				if(closest == system.getProcessId())
				{
					found = true;
					carrier = closest;
				}
				else 
				{
					system.sendMessage(closest, 3, new FindNodeMessage(hash, system.getProcessId(), -1));
					while(!confirmed) system.yield(); 
				}
			}
			
			confirmed = false;
		}
		
		while(!found) system.yield();
		found = false;
		
		int backup = backupCarrier(carrier);
		
		if(carrier == system.getProcessId())
		{
			values.put(hash, object);
			
			system.sendMessage(backup, 1, new HashMessage(hash, object));
			
			long time = System.currentTimeMillis();
			while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
			confirmed = false;
		}
		else
		{			
			system.sendMessage(carrier, 1, new HashMessage(hash, object));
			
			long time = System.currentTimeMillis();
			while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
			confirmed = false;
			
			if(backup == system.getProcessId()) values.put(hash, object);
			else
			{
				system.sendMessage(backup, 1, new HashMessage(hash, object));
				
				time = System.currentTimeMillis();
				while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
				confirmed = false;
			}
		}
	}

	@Override
	public Object get(int hash) {

		int closest = closestValue(hash, ids);
		
		if(closest == system.getProcessId())
		{
			found = true;
			carrier = closest;
		}
		else
		{
			system.sendMessage(closest, 3, new FindNodeMessage(hash, system.getProcessId(), -1));
			
			long time = System.currentTimeMillis();
			while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
			
			if(!confirmed)
			{
				closest = backupCarrier(closest);
				if(closest == system.getProcessId())
				{
					found = true;
					carrier = closest;
				}
				else
				{
					system.sendMessage(closest, 3, new FindNodeMessage(hash, system.getProcessId(), -1));
					while(!confirmed) system.yield(); 
				}
			}
			
			confirmed = false;
		}
		
		while(!found) system.yield();
		found = false;
		
		int backup = backupCarrier(carrier);
		
		if(carrier == system.getProcessId()) return values.get(hash);
		else 
		{						
			system.sendMessage(carrier, 2, hash);
			
			long time = System.currentTimeMillis();
			while(!confirmed && time + wait >= System.currentTimeMillis()) system.yield(); 
			
			if(!confirmed)
			{
				if(backup == system.getProcessId()) value = values.get(hash);
				else
				{
					system.sendMessage(backup, 2, hash);
					
					while(!confirmed) system.yield();
				}
			}
			
			confirmed = false;
			
			return value;
		}
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(CvetkovicMilanTTT.class, true, true, true);
	}
	
	class HashMessage {
		int key;
		Object value;
		
		public HashMessage(int key, Object value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return "HM : ("+key+", "+value+")";
		}
	}
	
	class FindNodeMessage {
		int hash;
		int id;
		int cfmdId;
		
		public FindNodeMessage(int hash, int id, int cfmdId) {
			this.hash = hash;
			this.id = id;
			this.cfmdId = cfmdId;
		}
		
		@Override
		public String toString() {
			return "FN : ("+hash+", "+id+", "+cfmdId+")";
		}
	}

}
