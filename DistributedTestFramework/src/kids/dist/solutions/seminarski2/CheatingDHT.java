package kids.dist.solutions.seminarski2;

import java.util.concurrent.ConcurrentHashMap;

import kids.dist.common.problem.InitiableSolution;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class CheatingDHT implements DistributedHashTable, InitiableSolution {
	static final ConcurrentHashMap<Integer, Object> map = new ConcurrentHashMap<Integer, Object>();
	static volatile int n;
	DistributedManagedSystem system;
	
	@Override
	public void messageReceived(int from, int type, Object message) {}
	
	@Override
	public void initialize() {
		if (n++ % system.getNumberOfNodes() == 0)
			map.clear();
	}
	
	@Override
	public void put(int hash, Object object) {
		map.put(hash, object);
	}
	
	@Override
	public Object get(int hash) {
		return map.get(hash);
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(CheatingDHT.class, 2, 2, true, true, true);
	}
}
