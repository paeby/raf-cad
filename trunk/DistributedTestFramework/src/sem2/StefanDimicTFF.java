package sem2;

import java.util.Arrays;
import java.util.HashMap;
import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class StefanDimicTFF implements DistributedHashTable, InitiableSolution {
	private DistributedSystem system;
	private HashMap<Integer, Object> map;

	private boolean putConfirmationArrived;

	private HashMap<Integer, Object> receivedData;
	
	int waittime = 200;

	@Override
	public void initialize() {
		map = new HashMap<Integer, Object>();
		receivedData = new HashMap<Integer, Object>();
	}

	private int getDataHolder(int hash) {
		int[] neighbourhood = system.getProcessNeighbourhood();
		int[] neighbourhoodAndMe = new int[neighbourhood.length + 1];
		neighbourhoodAndMe[0] = system.getProcessId();

		System.arraycopy(neighbourhood, 0, neighbourhoodAndMe, 1,
				neighbourhood.length);

		Arrays.sort(neighbourhoodAndMe);

		int index = hash % neighbourhoodAndMe.length;

		return neighbourhoodAndMe[index];
	}

	private void sendAndWaitGet(int to, int hash) {
		system.sendMessage(to, 2, new Transport(hash, null));
		long time = System.currentTimeMillis();

		while (!receivedData.containsKey(hash)) {
			system.yield();
			if (System.currentTimeMillis() - time > 200)
				return;
		}
	}

	@Override
	public Object get(int hash) {
		int holder = getDataHolder(hash);
		int backup = holder ^ 0x01;
		int myID = system.getProcessId();

		if (holder == myID || backup == myID)
			return map.get(hash);

		else {
			sendAndWaitGet(holder, hash);

			if (receivedData.containsKey(hash))
				return receivedData.remove(hash);

			sendAndWaitGet(backup, hash);

			if (receivedData.containsKey(hash))
				return receivedData.remove(hash);

			else
				return null;
		}
	}

	private void sendAndWaitPut(int to, int hash, Object object) {
		system.sendMessage(to, 0, new Transport(hash, object));

		long time = System.currentTimeMillis();

		while (!putConfirmationArrived) {
			system.yield();
			if (System.currentTimeMillis() - time > 200)
				break;
		}
		putConfirmationArrived = false;
	}

	@Override
	public void put(int hash, Object object) {
		int holder = getDataHolder(hash);
		int backup = holder ^ 0x01;
		int myID = system.getProcessId();

		putConfirmationArrived = false;

		if (holder == myID) {
			map.put(hash, object);
			sendAndWaitPut(backup, hash, object);

		} else if (backup == myID) {
			map.put(hash, object);
			sendAndWaitPut(holder, hash, object);
		}

		else {
			sendAndWaitPut(holder, hash, object);
			sendAndWaitPut(backup, hash, object);
		}
	}

	// 0 put
	// 1 put confirmation
	// 2 get
	// 3 get confirmation
	@Override
	public void messageReceived(int from, int type, Object message) {
		switch (type) {
		case 0:
			Transport t = (Transport) message;
			map.put(t.hash, t.data);

			system.sendMessage(from, 1, null);
			break;

		case 1:
			putConfirmationArrived = true;
			break;

		case 2:
			Transport transport = (Transport) message;
			Transport temp = new Transport(transport.hash, get(transport.hash));

			system.sendMessage(from, 3, temp);
			break;

		case 3:
			Transport tr = (Transport) message;
			receivedData.put(tr.hash, tr.data);
			break;
		}
	}

	private class Transport {
		private int hash;
		private Object data;

		private Transport(int hash, Object data) {
			this.hash = hash;
			this.data = data;
		}
	}

	public static void main(String[] args) {
		 DistributedHashTableTester.testDHT(StefanDimicTFF.class, true, false,
		 true);
//		DistributedHashTableTester.testDHT(StefanDimicTFF.class, 8, 4, true,
//				false, true);
	}
}
