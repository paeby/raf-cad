package sem2.milosstankovicTTT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class DistributedHashTableSolution implements DistributedHashTable,
		InitiableSolution {

	private static final int TIME_OUT = 1000;

	private DistributedSystem system;
	private ArrayList<Integer>[] buckets;
	private Map<Integer, Object> mapa;

	private Map<Integer, Object> responseMap;
	private int localTime;
	private int crashed = -1;

	@SuppressWarnings("unchecked")
	public DistributedHashTableSolution() {
		buckets = new ArrayList[KademliaUtil.NUMBER_BITS + 1];
		mapa = new HashMap<Integer, Object>();
		responseMap = new HashMap<Integer, Object>();
		localTime = 0;
	}

	@Override
	public void initialize() {
		int id = system.getProcessId();
		for (int n : system.getProcessNeighbourhood()) {
			int b = KademliaUtil.getBucket(id, n);
			if (buckets[b] == null)
				buckets[b] = new ArrayList<Integer>();
			buckets[b].add(n);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void messageReceived(int from, int type, Object messageWithTimestamp) {
		Pair<Integer, Object> msg = (Pair<Integer, Object>) messageWithTimestamp;
		int timestamp = msg.a;
		Object message = msg.b;

		Pair<Integer, Object> p;
		int time;
		switch (type) {
		case 0:
			responseMap.put(timestamp, message);
			break;
		case 1:
			int nearest = getLocalNearest((Integer) message);
			if (nearest == -1)
				nearest = system.getProcessId();
			system.sendMessage(from, 0, new Pair<Integer, Integer>(timestamp,
					nearest));
			break;
		case 2:
			long t1,
			t2;
			p = (Pair<Integer, Object>) message;
			mapa.put(p.a, p.b);

			time = ++localTime;
			if (crashed != (system.getProcessId() ^ 1)) {
				system.sendMessage(system.getProcessId() ^ 1, 3,
						new Pair<Integer, Object>(time, message));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						crashed = system.getProcessId() ^ 1;
						break;
					}
					system.yield();
				}
				responseMap.remove(time);
			}

			system.sendMessage(from, 0, new Pair<Integer, Boolean>(timestamp,
					true));
			break;
		case 3:
			p = (Pair<Integer, Object>) message;
			mapa.put(p.a, p.b);
			system.sendMessage(from, 0, new Pair<Integer, Boolean>(timestamp,
					true));
			break;
		case 4:
			int key = (Integer) message;
			Object value = mapa.get(key);
			system.sendMessage(from, 0, new Pair<Integer, Object>(timestamp,
					value));
			break;
		case 5:
			System.out.println("primljeno("
					+ Integer.toBinaryString(system.getProcessId()) + "): "
					+ timestamp + " " + message);
			break;
		}
	}

	@Override
	public void put(int hash, Object object) {
		int nearestId = getNearest(hash);
		long t1, t2;
		int time = -1;
		if (nearestId == -1) {
			mapa.put(hash, object);

			time = ++localTime;
			if (crashed != (system.getProcessId() ^ 1)) {
				system.sendMessage(system.getProcessId() ^ 1, 3,
						new Pair<Integer, Object>(time,
								new Pair<Integer, Object>(hash, object)));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						crashed = system.getProcessId() ^ 1;
						break;
					}
					system.yield();
				}
				responseMap.remove(time);
			}
		} else {
			if (crashed != nearestId) {
				time = ++localTime;
				system.sendMessage(nearestId, 2, new Pair<Integer, Object>(
						time, new Pair<Integer, Object>(hash, object)));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > 4 * TIME_OUT) {
						crashed = nearestId;
						break;
					}
					system.yield();
				}
			}
			if (!responseMap.containsKey(time)) {
				nearestId ^= 1;
				if (nearestId == system.getProcessId()) {
					mapa.put(hash, object);
					return;
				}
				time = ++localTime;
				system.sendMessage(nearestId, 3, new Pair<Integer, Object>(
						time, new Pair<Integer, Object>(hash, object)));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						System.out.println("NEMOGUCE put");
						break;
					}
					system.yield();
				}
			}
			responseMap.remove(time);
		}
	}

	@Override
	public Object get(int hash) {
		int nearestId = getNearest(hash);
		if (nearestId == -1) {
			return mapa.get(hash);
		} else {
			long t1, t2;
			int time = -1;
			if (crashed != nearestId) {
				time = ++localTime;
				system.sendMessage(nearestId, 4, new Pair<Integer, Integer>(
						time, hash));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						crashed = nearestId;
						break;
					}
					system.yield();
				}
			}
			if (!responseMap.containsKey(time)) {
				nearestId ^= 1;
				if (nearestId == system.getProcessId())
					return mapa.get(hash);
				time = ++localTime;
				system.sendMessage(nearestId, 4, new Pair<Integer, Integer>(
						time, hash));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						System.out.println("NEMOGUCE get");
						break;
					}
					system.yield();
				}
			}
			Object value = responseMap.get(time);
			responseMap.remove(time);
			return value;
		}
	}

	private int getLocalNearest(int x) {
		int p = -1;
		int dis = KademliaUtil.getDistance(system.getProcessId(), x);
		for (int n : system.getProcessNeighbourhood()) {
			int d = KademliaUtil.getDistance(n, x);
			if (d < dis) {
				p = n;
				dis = d;
			}
		}
		return p;
	}

	private int getNearest(int x) {
		int nearest = getLocalNearest(x);
		long t1, t2;
		int time = -1;
		int mapValue;
		if (nearest == -1)
			return -1;
		if ((nearest ^ 1) == system.getProcessId())
			return nearest;
		do {
			if (crashed != nearest) {
				time = ++localTime;
				system.sendMessage(nearest, 1, new Pair<Integer, Integer>(time,
						x));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						crashed = nearest;
						break;
					}
					system.yield();
				}
			}
			if (!responseMap.containsKey(time)) {
				time = ++localTime;
				system.sendMessage(nearest ^ 1, 1, new Pair<Integer, Integer>(
						time, x));
				t1 = System.currentTimeMillis();
				while (!responseMap.containsKey(time)) {
					t2 = System.currentTimeMillis();
					if (t2 - t1 > TIME_OUT) {
						System.err.println("NEMOGUCE nearest");
						break;
					}
					system.yield();
				}
			}
			mapValue = (Integer) (responseMap.get(time));
			responseMap.remove(time);
			if (nearest == mapValue)
				break;
			nearest = mapValue;
		} while (true);
		return nearest;
	}

	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(DistributedHashTableSolution.class,
				true, true, true);
	}
}
