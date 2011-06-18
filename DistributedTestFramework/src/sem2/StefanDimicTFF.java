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

	private boolean putConfirmationSent;

	private HashMap<Integer, Object> recieved;

	@Override
	public void initialize() {
		map = new HashMap<Integer, Object>();
		recieved = new HashMap<Integer, Object>();
	}

	private int findObjectHolder(int hash) {
		int[] neighbourhood = system.getProcessNeighbourhood();
		int[] neighbourhoodAndMe = new int[neighbourhood.length + 1];
		neighbourhoodAndMe[0] = system.getProcessId();

		System.arraycopy(neighbourhood, 0, neighbourhoodAndMe, 1,
				neighbourhood.length);

		Arrays.sort(neighbourhoodAndMe);

		int index = hash % neighbourhoodAndMe.length;

		return neighbourhoodAndMe[index];
	}

	@Override
	public Object get(int hash) {
		int holder = findObjectHolder(hash);

		if (holder == system.getProcessId())
			return map.get(hash);
		else {
			system.sendMessage(holder, 2, new Transport(hash, null));

			while (!recieved.containsKey(hash))
				system.yield();

			return recieved.remove(hash);
		}
	}

	@Override
	public void put(int hash, Object object) {
		int holder = findObjectHolder(hash);

		if (holder == system.getProcessId()) {
			map.put(hash, object);
		} else {
			system.sendMessage(holder, 0, new Transport(hash, object));

			while (!putConfirmationSent)
				system.yield();

			putConfirmationSent = false;
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
			put(t.hash, t.data);

			system.sendMessage(from, 1, null);
			break;

		case 1:
			putConfirmationSent = true;
			break;

		case 2:
			Transport transport = (Transport) message;
			transport.data = get(transport.hash);
			// Transport temp = new Transport(transport.hash,
			// get(transport.hash));

			system.sendMessage(from, 3, transport);
			break;

		case 3:
			Transport tr = (Transport) message;
			recieved.put(tr.hash, tr.data);
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
		DistributedHashTableTester.testDHT(StefanDimicTFF.class, true, false, false);
	}
}
