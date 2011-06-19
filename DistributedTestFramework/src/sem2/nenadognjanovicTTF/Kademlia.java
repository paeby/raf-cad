package sem2.nenadognjanovicTTF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class Kademlia implements DistributedHashTable, InitiableSolution {

	public DistributedSystem s;

	public static enum State {
		Idle, Putting, Getting, LookingForNode
	};

	private State state = State.Idle;

	private int myID;
	private ArrayList<Integer>[] buckets;

	private HashMap<Integer, Object> data;

	private int lookingForNodeID;
	private int closestNodeFound = Integer.MIN_VALUE;

	private Object gottenObject;
	private boolean storeOK;
	private boolean getOK;


	@Override
	public void initialize() {
		myID = s.getProcessId();
		data = new HashMap<Integer, Object>();

		buildBuckets();
	}


	private void buildBuckets() {
		buckets = new ArrayList[K.KEY_BITS];
		// create empty buckets
		for (int i = 0; i < buckets.length; i++) {
			buckets[i] = new ArrayList<Integer>();
		}
		// fill buckets with known nodes from neighbourhood
		for (int neighID : s.getProcessNeighbourhood()) {
			addToBucket(neighID);
		}
		// check
		/*for(int i=0; i<buckets.length; i++) {
			if(buckets[i].size()==0)
				System.out.println(">> Node: " + myID + "  bucket " + i + " empty");
		}
		System.out.println();*/
	}


	private void addToBucket(int nodeID) {
		if (nodeID == myID) {
			System.out.println("Warning - id being added to bucket is the same as mine - " + myID);
			return;
		}
		int b = K.getBucketForID(myID, nodeID);
		if (!buckets[b].contains(nodeID))
			buckets[b].add(nodeID);
	}


	@Override
	public void put(int hash, Object object) {
		if (hash == myID) {
			data.put(hash, object);
			return;
		}
		lookingForNodeID = hash;

		int localClosest = K.getClosestLocallyKnownNode(myID, hash, buckets);
		//System.out.println("STORE @ " + myID + " - hash: " + hash + "  local closest: " + localClosest);
		s.sendMessage(localClosest, K.MSG_FIND_CLOSEST_NODE, hash);

		while (closestNodeFound == Integer.MIN_VALUE) {
			s.yield();
		}
		//System.out.println("STORE @ " + myID + " - hash: " + hash + "  global closest: " + closestNodeFound);

		if (closestNodeFound == myID) {
			data.put(hash, object);
		} else {
			StoreMessage smsg = new StoreMessage(hash, object);
			s.sendMessage(closestNodeFound, K.MSG_STORE, smsg);
			while (!storeOK) {
				s.yield();
			}
		}

		storeOK = false;
		lookingForNodeID = Integer.MIN_VALUE;
		closestNodeFound = Integer.MIN_VALUE;
	}


	@Override
	public Object get(int hash) {
		if (hash == myID) {
			return data.get(hash);
		}
		lookingForNodeID = hash;

		int localClosest = K.getClosestLocallyKnownNode(myID, hash, buckets);
		//System.out.println("GET   @ " + myID + " - hash: " + hash + "  local closest:" + localClosest);
		s.sendMessage(localClosest, K.MSG_FIND_CLOSEST_NODE, hash);

		while (closestNodeFound == Integer.MIN_VALUE) {
			s.yield();
		}
		//System.out.println("GET   @ " + myID + " - hash: " + hash + "  global closest:" + closestNodeFound);

		if (closestNodeFound == myID) {
			gottenObject = data.get(hash);
		} else {
			s.sendMessage(closestNodeFound, K.MSG_GET, hash);
			while (!getOK) {
				s.yield();
			}
		}

		Object temp = gottenObject;

		getOK = false;
		gottenObject = null;
		closestNodeFound = Integer.MIN_VALUE;
		lookingForNodeID = Integer.MIN_VALUE;
		return temp;
	}


	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == K.MSG_FIND_CLOSEST_NODE) {
			int requestedNodeID = (Integer) message;
			if (requestedNodeID == myID) { // I'm the one
				s.sendMessage(from, K.MSG_CLOSEST_REPLY, myID);
			} else { // return the one that might be the better candidate
				int closestID = K.getClosestLocallyKnownNode(myID, requestedNodeID, buckets);
				if (K.distance(requestedNodeID, myID) < K.distance(requestedNodeID, closestID))
					s.sendMessage(from, K.MSG_CLOSEST_REPLY, myID);
				else
					s.sendMessage(from, K.MSG_CLOSEST_REPLY, closestID);
			}
		}

		if (type == K.MSG_CLOSEST_REPLY) {
			int repliedNodeID = (Integer) message;

			if (repliedNodeID != myID) { // remember replied nodeID in bucket
				addToBucket(repliedNodeID);
			}

			if (repliedNodeID == myID) { // finish the lookup
				closestNodeFound = repliedNodeID;
			} else if (repliedNodeID == from) {
				int locallyClosest = K.getClosestLocallyKnownNode(myID, lookingForNodeID, buckets);
				if (K.distance(lookingForNodeID, locallyClosest) < K.distance(lookingForNodeID, repliedNodeID))
					closestNodeFound = locallyClosest;
				else
					closestNodeFound = repliedNodeID;
			} else { // try to find closer
				s.sendMessage(repliedNodeID, K.MSG_FIND_CLOSEST_NODE, lookingForNodeID);
			}

		}

		if (type == K.MSG_STORE) {
			//System.out.println(myID + " received STORE request from " + from);
			StoreMessage smsg = (StoreMessage) message;
			data.put(smsg.hash, smsg.object);
			s.sendMessage(from, K.MSG_STORE_REPLY, "OK");
		}
		if (type == K.MSG_STORE_REPLY) {
			if ("OK".equals((String) message))
				storeOK = true;
		}

		if (type == K.MSG_GET) {
			//System.out.println(myID + " received GET request from " + from + " for hash " + message);
			int hash = (Integer) message;
			if (!data.containsKey(hash)) {
				//System.out.println("Node " + from + " sent GET request to node " + myID + " with hash " + hash + " but it has no object stored under that key");
			}
			s.sendMessage(from, K.MSG_GET_REPLY, data.get(hash));
		}
		if (type == K.MSG_GET_REPLY) {
			gottenObject = message;
			getOK = true;
		}

	}

	private class StoreMessage {
		public int hash;
		public Object object;


		public StoreMessage(int hash, Object object) {
			this.hash = hash;
			this.object = object;
		}


		public String toString() {
			return "STORE h=" + hash + " obj=" + object;
		}
	}


	public static void main(String[] args) {
		System.out.println("KiDS seminarski 2 - Kademlia za 14 poena - Nenad Ognjanovic 61/05");
		DistributedHashTableTester.testDHT(Kademlia.class, K.KEY_BITS, 16, true, true, false);
	}

}