package kids.dist.solutions.seminarski2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import kids.dist.common.problem.InitiableSolution;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

/**
 * Radi samo overwrite
 * @author Bocete
 *
 */
public class BasicDHT implements DistributedHashTable, InitiableSolution {
	
	DistributedManagedSystem system;
	int myId, myIndex;
	int[] neighborhood;
	Map<Integer, Object> map = new HashMap<Integer, Object>();
	final Request request = new Request();
	Object response;
	
	@Override
	public void initialize() {
		myId = system.getProcessId();
		neighborhood = system.getProcessNeighbourhood();
		myIndex = -Arrays.binarySearch(neighborhood, myId) - 1;
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == 0) {
			Request request = (Request) message;
			map.put(request.hash, request.object);
			system.sendMessage(from, 1, null);
		} else if (type == 1) {
			response = true;
		} else if (type == 2) {
			system.sendMessage(from, 3, map.get((Integer) message));
		} else if (type == 3) {
			response = message;
		}
	}
	
	@Override
	public void put(int hash, Object object) {
		int solIndex = hash % (neighborhood.length + 1);
		if (solIndex == myIndex)
			map.put(hash, object);
		else {
			if (solIndex > myIndex)
				solIndex--;
			response = null;
			system.sendMessage(neighborhood[solIndex], 0, request.put(hash, object));
			while (response == null)
				system.yield();
		}
	}
	
	@Override
	public Object get(int hash) {
		int solIndex = hash % (neighborhood.length + 1);
		if (solIndex == myIndex)
			return map.get(hash);
		else {
			if (solIndex > myIndex)
				solIndex--;
			response = this;
			system.sendMessage(neighborhood[solIndex], 2, hash);
			while (response == this)
				system.yield();
			return response;
		}
	}
	
	class Request {
		volatile int hash;
		volatile Object object;
		
		Request put(int hash, Object object) {
			this.hash = hash;
			this.object = object;
			return this;
		}
		
		@Override
		public String toString() {
			return "[" + hash + ", " + object + "]";
		}
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(BasicDHT.class, true, false, false);
	}
}
