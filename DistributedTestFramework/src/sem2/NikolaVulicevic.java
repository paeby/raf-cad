package sem2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class NikolaVulicevic implements DistributedHashTable, InitiableSolution {
	private ArrayList<Integer> nodes = new ArrayList<Integer>();
	private HashMap<Integer, Object> data = new HashMap<Integer, Object>();
	private DistributedSystem system;
	private boolean gotResponse = false;
	private Object response;
	private boolean debug = false;
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		// remote put & get, type = hash
		if (type >= 0 && message == null) { // get
			system.sendMessage(from, -1, data.get(type));
		} else if (type >= 0 && message != null) { // put
			data.put(type, message);
			system.sendMessage(from, -1, null);
		}
		
		if (type == -1) { // response
			response = message;
			gotResponse = true;
		}
	}
	
	@Override
	public void put(int hash, Object object) {
		int node = getNode(hash);
		if (debug)
			System.out.println("Putting in node: " + node + " , Hash: " + hash);
		if (node == -1) {
			data.put(hash, object);
		} else {
			sendAndWait(node, hash, object);
		}
	}
	
	@Override
	public Object get(int hash) {
		int node = getNode(hash);
		if (debug)
			System.out.println("Getting from node: " + node + " , Hash: " + hash);
		if (node == -1) {
			return data.get(hash);
		} else {
			sendAndWait(node, hash, null);
			return response;
		}
	}
	
	public void sendAndWait(int toID, int hash, Object obj) {
		system.sendMessage(nodes.get(toID), hash, obj);
		gotResponse = false;
		while (!gotResponse) {
			system.yield();
		}
		gotResponse = false;
	}
	
	public int getNode(int hash) {
		int len = (int) Math.floor(256 / (nodes.size()));
		int pos = hash % len;
		if (debug)
			System.out.println("POS: " + pos + " / LEN: " + len);
		if (nodes.get(pos) == system.getProcessId()) {
			return -1;
		}
		return pos;
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
		DistributedHashTableTester.testDHT(NikolaVulicevic.class, true, false, false);
	}
}
