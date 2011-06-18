package sem2;

import java.util.ArrayList;
import java.util.HashMap;
import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class MarkoSavicTFT implements DistributedHashTable, InitiableSolution {
	int bocetimetout = 400;
	
	
	boolean received = false;
	
	ArrayList<Integer> list = new ArrayList<Integer>();
	HashMap<Integer, Object> myHashMap = new HashMap<Integer, Object>();
	
	Object hashObject;
	DistributedSystem system;
	
	@Override
	public void initialize() {
		int i = 0;
		int brojac = 0;
		
		while (i < system.getProcessNeighbourhood().length) {
			if (brojac == 0) {
				if (system.getProcessId() < system.getProcessNeighbourhood()[i]) {
					list.add(system.getProcessId());
					brojac++;
					
				}
			}
			
			list.add(system.getProcessNeighbourhood()[i]);
			i++;
			if (brojac == 0 && i == system.getProcessNeighbourhood().length) {
				list.add(system.getProcessId());
				
			}
		}
		
	}
	
	@Override
	public void put(int hash, Object object) {
		
		int processId;
		int myNeighbour;
		
		int randomPosition = (hash % list.size()); // random nod
		
		if (randomPosition == (list.size() - 1))
			myNeighbour = list.get(0); // ako je random nod poslednji u
										// listi,njegov bekap komsija ce biti
										// prvi u listi
		else
			myNeighbour = list.get(randomPosition + 1); // u suprotnom bekap
														// komsija ce mu biti
														// prvi do njega
			
		if (list.get(randomPosition) == system.getProcessId()) // ako sam ja
																// random nod
		{
			
			system.sendMessage(myNeighbour, 0, new Object[] { hash, object }); // bekap
																				// cuvamo
																				// kod
																				// komsije
																				// u
																				// listu
			long time = System.currentTimeMillis();
			while (!received && time + bocetimetout >= System.currentTimeMillis())
				system.yield();
			
			if (received)
				received = false;
			myHashMap.put(hash, object); // stavljamo kod mene u listu
		}

		else // ako je neko drugi random nod
		{
			processId = list.get(randomPosition);
			system.sendMessage(processId, 0, new Object[] { hash, object }); // stavljamo
																				// kod
																				// njega
																				// u
																				// listu
			long time = System.currentTimeMillis();
			while (received != true && time + bocetimetout >= System.currentTimeMillis())
				system.yield();
			if (received)
				received = false;
			
			if (myNeighbour == system.getProcessId())
				myHashMap.put(hash, object); // ako sam ja bekap
												// komsija,stavljamo kod mene
				
			else {
				system.sendMessage(myNeighbour, 0, new Object[] { hash, object }); // ako
																					// nisam,stavljamo
																					// kod
																					// regularnog
																					// bekap
																					// komsije
				time = System.currentTimeMillis();
				while (received != true && time + bocetimetout >= System.currentTimeMillis())
					system.yield();
				if (received)
					received = false;
			}
			
		}
		
	}
	
	@Override
	public Object get(int hash) {
		
		int processId;
		int myNeighbour;
		
		int randomPosition = (hash % list.size()); // uzimamo random noda
		
		if (list.get(randomPosition).equals(system.getProcessId()))
			return myHashMap.get(hash); // ako sam to ja,uzimamo od mene
			
		else // ako to nisam ja
		{
			
			processId = list.get(randomPosition);
			if (randomPosition == (list.size() - 1))
				myNeighbour = list.get(0); // ako je random nod poslednji u
											// listi,njegov bekap komsija ce
											// biti prvi u listi
			else
				myNeighbour = list.get(randomPosition + 1); // u suprotnom bekap
															// komsija ce mu
															// biti prvi do
															// njega
				
			system.sendMessage(processId, 1, hash); // uzimamo od obicnog noda
			long time = System.currentTimeMillis();
			while (!received && time + bocetimetout >= System.currentTimeMillis())
				system.yield();
			
			if (!received) // ako je zaginuo
			{
				
				if (myNeighbour == system.getProcessId())
					return myHashMap.get(hash); // a ja sam bekap komsija,uzimam
												// od mene
				else {
					system.sendMessage(myNeighbour, 1, hash); // neko drugi je
																// bekap
																// komsija,uzimamo
																// od njega
					while (received != true)
						system.yield();
				}
				
			}
			received = false;
			
		}
		return hashObject;
		
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		
		if (type == 0) {
			Object[] array = (Object[]) message;
			myHashMap.put((Integer) array[0], array[1]);
			system.sendMessage(from, 2, null);
		}
		if (type == 1) {
			Object foundObject = myHashMap.get((Integer) message);
			system.sendMessage(from, 2, foundObject);
		}
		if (type == 2) {
			hashObject = message;
			received = true;
		}
		
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(MarkoSavicTFT.class, true, false, true);
		
	}
	
}
