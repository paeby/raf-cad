package sem2.nikolavulicevicTTT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;


public class DHTImpl implements DistributedHashTable, InitiableSolution {
	private HashMap<Integer, Object> data = new HashMap<Integer, Object>();
	private DistributedSystem system;
	private ArrayList<Integer> nodes = new ArrayList<Integer>();
	private boolean gotResponse = false;
	private Object response;
	private long time;
	private int retClosestID = -1;


	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == -1) { // response
			response = message;
			gotResponse = true;
			return;
		} else if (type == -2) { // kademlia response
			retClosestID = (Integer) message;
			gotResponse = true;
			return;
		}

		// remote put & get
		Message msg = (Message) message;
		if(type == 1) { // get
			system.sendMessage(from, -1, data.get(msg.getHash()));
		} 

		else if (type == 2) { // put
			data.put(msg.getHash(), msg.getObj());
			system.sendMessage(from, -1, null);
		} 

		else if (type == 3) { // kademlia
			system.sendMessage(from, -2, findClosest(msg.getHash()));
		}
	}



	@Override
	public void put(int hash, Object object) {
		int node = getNode(hash);
		if(node == -1) {
			data.put(hash, object);
			backup(system.getProcessId(), hash, object);
		} else {
			sendAndWait(node, 2, hash, object);
			backup(node, hash, object);
		}
	}


	@Override
	public Object get(int hash) {
		response = null;
		int node = getNode(hash);
		if(node == -1) {
			return data.get(hash);
		} else {
			sendAndWait(node, 1, hash, null);
			if(response == null) {
				int partner = node ^ 0x01;
				if(partner == system.getProcessId()) {
					return data.get(hash);
				}
				sendAndWait(partner, 1, hash, null);
			}
			return response;
		}
	}

	private void backup(int id, int hash, Object object) {
		int partner = id ^ 0x01;
		if(partner == system.getProcessId()) {
			data.put(hash, object);
		} else {
			sendAndWait(partner, 2, hash, object);
		}
	}

	public void sendAndWait(int to, int type, int hash, Object obj) {
		time = System.currentTimeMillis();
		system.sendMessage(to, type, new Message(hash, obj));
		gotResponse = false;
		while(!gotResponse) {
			system.yield();
			if (System.currentTimeMillis() - time > 50) break;
		}
		gotResponse = false;
	}

	public int getNode(int hash) {
		int currClosestID = findClosest(hash);

		do {
			if(currClosestID != system.getProcessId()) {
				retClosestID = -1;
				sendAndWait(currClosestID, 3, hash, null);
				if(retClosestID == -1) {
					if((currClosestID ^ 0x01) != system.getProcessId()) {
						sendAndWait(currClosestID ^ 0x01, 3, hash, null);
					} else {
						break;
					}
				}
				if((retClosestID ^ hash) < (currClosestID ^ hash)) {
					currClosestID = retClosestID;
				} else {
					break;
				}
			} else {
				currClosestID = -1;
				break;
			}
		} while (true);

		return currClosestID;
	}

	public int findClosest(int hash) {
		int distance = Integer.MAX_VALUE;
		int id = 0;
		for (int i = 0; i < nodes.size(); i++) {
			if((nodes.get(i) ^ hash) < distance) {
				distance = nodes.get(i) ^ hash;
				id = nodes.get(i);
			}
		}
		return id;
	}

	@Override
	public void initialize() {
		for (int i = 0; i < system.getProcessNeighbourhood().length; i++) {
			nodes.add(system.getProcessNeighbourhood()[i]);
		}
		nodes.add(system.getProcessId());
		Collections.sort(nodes);
	}

	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(DHTImpl.class, true, true, true);
	}
}