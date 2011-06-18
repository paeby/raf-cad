package sem2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class MiroslavBogdanovicTTT implements DistributedHashTable, InitiableSolution {
	
	DistributedSystem system;
	
	static final long DELAY = 600;
	
	int myId;
	
	boolean recvPutAck;
	
	Object getObject;
	
	HashMap<Integer, Object> localValues;
	HashMap<Integer, Integer> closestMap;
	HashSet<Integer> closestAckSet;
	HashMap<Integer, Object> getValues;
	
	class Value {
		int hash;
		Object object;
		
		public Value(int hash, Object object) {
			this.hash = hash;
			this.object = object;
		}
		
	}
	
	class HashAndDelay {
		int hash;
		long delay;
		
		public HashAndDelay(int hash, long delay) {
			
			this.hash = hash;
			this.delay = delay;
		}
		
	}
	
	class HashAndClosest {
		int hash;
		int closestId;
		
		public HashAndClosest(int hash, int closestId) {
			this.hash = hash;
			this.closestId = closestId;
		}
		
	}
	
	@Override
	public void initialize() {
		myId = system.getProcessId();
		localValues = new HashMap<Integer, Object>();
		closestMap = new HashMap<Integer, Integer>();
		closestAckSet = new HashSet<Integer>();
		getValues = new HashMap<Integer, Object>();
	}
	
	private void mySendMessage(int destinationId, int type, Object message) {
		if (destinationId != myId)
			system.sendMessage(destinationId, type, message);
		else
			messageReceived(myId, type, message);
	}
	
	private int getClosestId(int hash) {
		if (closestMap.containsKey(hash))
			return closestMap.get(hash);
		int[] neighbours = system.getProcessNeighbourhood();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int nbId : neighbours)
			list.add(nbId);
		while (true) {
			int best = myId;
			for (int nbId : list) {
				if ((nbId ^ hash) < (best ^ hash))
					best = nbId;
			}
			
			if ((best ^ hash) < (myId ^ hash)) {
				closestAckSet.remove(best);
				mySendMessage(best, 0, hash);
				
				long time = System.currentTimeMillis();
				while (System.currentTimeMillis() - time < DELAY && !closestAckSet.contains(best))
					system.yield();
				
				if (!closestAckSet.contains(best)) {
					int i = 0;
					while (list.get(i) != best)
						i++;
					list.remove(i);
				}

				else {
					while (!closestMap.containsKey(hash))
						system.yield();
					
					return closestMap.get(hash);
				}
				
			} else
				return myId;
		}
		
	}
	
	@Override
	public Object get(int hash) {
		int loc = getClosestId(hash);
		
		getValues.remove(hash);
		
		mySendMessage(loc, 4, hash);
		mySendMessage(loc ^ 1, 4, hash);
		
		while (!getValues.containsKey(hash))
			system.yield();
		
		Object object = getValues.get(hash);
		getValues.remove(hash);
		return object;
		
	}
	
	@Override
	public void put(int hash, Object object) {
		int loc = getClosestId(hash);
		
		Value value = new Value(hash, object);
		
		recvPutAck = false;
		
		mySendMessage(loc, 2, value);
		mySendMessage(loc ^ 1, 2, value);
		
		while (!recvPutAck)
			system.yield();
		
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() - time < DELAY)
			system.yield();
		
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == 0) {
			mySendMessage(from, 6, null);
			mySendMessage(from, 1, new HashAndClosest((Integer) message, getClosestId((Integer) message)));
		}
		
		if (type == 1) {
			HashAndClosest hashAndClosest = (HashAndClosest) message;
			closestMap.put(hashAndClosest.hash, hashAndClosest.closestId);
		}
		if (type == 2) {
			Value value = (Value) message;
			localValues.put(value.hash, value.object);
			mySendMessage(from, 3, null);
		}
		if (type == 3)
			recvPutAck = true;
		if (type == 4)
			mySendMessage(from, 5, new Value((Integer) message, localValues.get((Integer) message)));
		if (type == 5) {
			Value value = (Value) message;
			getValues.put(value.hash, value.object);
		}
		if (type == 6)
			closestAckSet.add(from);
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(MiroslavBogdanovicTTT.class, true, true, true);
	}
	
}
