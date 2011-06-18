package sem2.igorjovicTFT;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class DHTSolution implements DistributedHashTable, InitiableSolution {
	
	static int TIMEOUT = 400;
	
	DistributedSystem system;
	
	TreeMap<Integer, Object> map = new TreeMap<Integer, Object>();
	TreeMap<Integer, Object> receivedList = new TreeMap<Integer, Object>();
	LinkedList<Integer> deadList = new LinkedList<Integer>();
	boolean ack = false;
	
	int myid;
	int[] neighbourhood;
	int[] allInGraph;
	
	@Override
	public void initialize() {
		myid = system.getProcessId();
		neighbourhood = system.getProcessNeighbourhood();
		allInGraph = new int[neighbourhood.length + 1];
		for (int i = 0; i < neighbourhood.length; i++) {
			allInGraph[i] = neighbourhood[i];
		}
		allInGraph[neighbourhood.length] = myid;
		Arrays.sort(allInGraph);
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		
		if (type == 1) { // stigao put za mene
			Pair pair = (Pair) message;
			// System.out.println(system.getProcessId()+"\tstigla poruka od "+from+" da stavim:"+pair.key);
			
			map.put(pair.key, pair.value);
			system.sendMessage(from, -1, "ack");
		}
		if (type == 2) { // stigao zahtev za vrednoscu
			int key = ((Pair) message).key;
			// System.out.println(system.getProcessId()+"\tstigla poruka od "+from+" da neko trazi:"+key);
			
			Object value = map.get(key);
			system.sendMessage(from, 3, new Pair(key, value));
		}
		if (type == 3) { // stigla vrednost
			if (deadList.contains(from))
				return;
			Pair p = (Pair) message;
			// System.out.println(system.getProcessId()+"\tdobio sam vrednost:"+p.key+" od "+from);
			receivedList.put(p.key, p.value);
		}
		
		if (type == -1) {// stigao ack
			ack = true;
		}
		
	}
	
	@Override
	public void put(int hash, Object object) {
		ack = false;
		int id = hashToId(hash);
		
		if (deadList.contains(id)) {
			// System.out.println(system.getProcessId()+"\tput:"+hash+" into "+id+" which is dead");
			putBackup(hash, object);
			return;
		}
		
		// System.out.println(system.getProcessId()+"\tput:"+hash+" into "+id);
		
		// System.out.println(system.getProcessId()+"\tsaljem poruku procesu:"+id+" da stavi:"+hash);
		if (id == myid) {
			map.put(hash, object);
			putBackup(hash, object);
			return;
		} else {
			system.sendMessage(hashToId(hash), 1, new Pair(hash, object));
		}
		
		long oldtime = System.currentTimeMillis();
		while (!ack) {
			system.yield();
			long newtime = System.currentTimeMillis();
			if (newtime - oldtime > TIMEOUT) {
				// System.out.println(system.getProcessId()+"\tumro nod:"+id);
				deadList.add(id);
				break;
			}
		}
		
		putBackup(hash, object);
	}
	
	public void putBackup(int hash, Object object) {
		ack = false;
		int id = hashToBackupId(hash);
		
		if (deadList.contains(id)) {
			// System.out.println(system.getProcessId()+"\tput backup:"+hash+" into "+id+" which is dead");
			return;
		}
		
		// System.out.println(system.getProcessId()+"\tput backup:"+hash+" into "+id);
		
		// System.out.println(system.getProcessId()+"\tsaljem poruku procesu:"+id+" da stavi u bkp:"+hash);
		if (id == myid) {
			map.put(hash, object);
			return;
		} else {
			system.sendMessage(id, 1, new Pair(hash, object));
		}
		
		long oldtime = System.currentTimeMillis();
		while (!ack) {
			system.yield();
			long newtime = System.currentTimeMillis();
			if (newtime - oldtime > TIMEOUT) {
				// System.out.println(system.getProcessId()+"\tumro nod:"+id);
				deadList.add(id);
				break;
			}
		}
	}
	
	@Override
	public Object get(int hash) {
		
		int id = hashToId(hash);
		
		if (deadList.contains(id)) {
			// System.out.println(system.getProcessId()+"\tget:"+hash+" from "+id+" which is dead");
			return getBackup(hash);
		}
		
		// System.out.println(system.getProcessId()+"\tget:"+hash+" from "+id);
		
		if (id == myid) {
			return map.get(hash);
		} else {
			system.sendMessage(id, 2, new Pair(hash, null));
		}
		
		long oldtime = System.currentTimeMillis();
		while (!receivedList.containsKey(hash)) {
			system.yield();
			long newtime = System.currentTimeMillis();
			if (newtime - oldtime > TIMEOUT) {
				// System.out.println(system.getProcessId()+"\tumro nod:"+id);
				deadList.add(id);
				return getBackup(hash);
			}
		}
		// Pair p = receivedList.get(hash);
		Object got = receivedList.remove(hash);
		
		return got;
		
	}
	
	public Object getBackup(int hash) {
		int id = hashToBackupId(hash);
		
		// System.out.println(system.getProcessId()+"\tget backup:"+hash+" from "+id);
		
		if (id == myid) {
			return map.get(hash);
		} else {
			system.sendMessage(id, 2, new Pair(hash, null));
		}
		
		while (!receivedList.containsKey(hash)) {
			system.yield();
		}
		// Pair p = receivedList.get(hash);
		Object got = receivedList.remove(hash);
		
		return got;
		
	}
	
	public int hashToId(int hash) {
		int length = allInGraph.length;
		
		int index = hash % length;
		return allInGraph[index];
	}
	
	public int hashToBackupId(int hash) {
		int length = allInGraph.length;
		
		int index = (hash % length + 1) % length;
		
		// if (index== allInGraph.length) index=0;
		return allInGraph[index];
	}
	
	public static void main(String[] args) {
		int bits = 8;
		int threads = 4;
		boolean testOverwrite = true;
		boolean testKademlia = false;
		boolean testSafety = true;
		
		// DistributedHashTableTester.testDHT(DHTSolution.class, bits, threads,
		// testOverwrite, testKademlia, testSafety);
		
		DistributedHashTableTester.testDHT(DHTSolution.class, testOverwrite, testKademlia, testSafety);
	}
	
}
