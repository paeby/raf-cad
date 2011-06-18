package sem2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class AleksandarJocicTFF implements DistributedHashTable, InitiableSolution {
	DistributedSystem system;
	TreeMap<Integer, Object> data = new TreeMap<Integer, Object>();
	int myId;
	ArrayList<Integer> Allnodes = new ArrayList<Integer>();
	boolean ackPut = false;
	TreeMap<Integer, Object> recieved = new TreeMap<Integer, Object>();
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == 0) {
			Object[] mess = (Object[]) message;
			int hash = (Integer) mess[0];
			Object object = mess[1];
			
			data.put(hash, object);
			if (from != myId)
				system.sendMessage(from, 1, null);
		} else if (type == 1) {
			ackPut = true;
		} else if (type == 2) {
			
			int hash = (Integer) message;
			system.sendMessage(from, 3, new Object[] { hash, get(hash) });
		} else if (type == 3) {
			Object[] mess = (Object[]) message;
			int hash = (Integer) mess[0];
			Object object = mess[1];
			recieved.put(hash, object);
		}
		
	}
	
	@Override
	public void put(int hash, Object object) {
		
		int idtoSend = findR(hash);
		if (idtoSend == myId) {
			messageReceived(myId, 0, new Object[] { hash, object });
			return;
		} else {
			ackPut = false;
			system.sendMessage(idtoSend, 0, new Object[] { hash, object, myId });
			
			while (!ackPut) {
				
				system.yield();
			}
			
		}
		
	}
	
	@Override
	public Object get(int hash) {
		int id = findR(hash);
		if (id == system.getProcessId())
			return data.get(hash);
		else {
			system.sendMessage(id, 2, hash);
			
			while (!recieved.containsKey(hash))
				system.yield();
			
			return recieved.remove(hash);
		}
		
	}
	
	public int findR(int hash) {
		int indexOfR = hash % Allnodes.size();
		return Allnodes.get(indexOfR);
		
	}
	
	@Override
	public void initialize() {
		myId = system.getProcessId();
		
		for (int node : system.getProcessNeighbourhood()) {
			
			Allnodes.add(node);
		}
		Allnodes.add(myId);
		
		Collections.sort(Allnodes);
		
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(AleksandarJocicTFF.class, true, false, false);
	}
}
