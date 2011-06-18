package sem2.damirtrhuljTFT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;

// message type 1 je objekat koji se salje da se upise
// message type 2 je ack za je poruka upisana
// message type 3 je get zahtev
// message type 4 je rezultat od geta
//
//
public class DamirSeminarski2 implements DistributedHashTable,
		InitiableSolution {
	DistributedSystem system;
	TreeMap<Integer, Object> bunker = new TreeMap<Integer, Object>();
	TreeMap<Integer, Object> pristiglo = new TreeMap<Integer, Object>();
	ArrayList<Integer> krepali = new ArrayList<Integer>();

	int[] svi;
	boolean poslataPorukaJeUpisana = false;
	final long cekanje = 800;

	@Override
	public Object get(int hash) {
		//System.out.println("get: " + hash);
		int od = hash % svi.length;
		int odBackup = backup(od);

		if (!krepali.contains(new Integer(svi[od]))) {
			if (svi[od] == system.getProcessId()) {
				pristiglo.put(hash, bunker.get(hash));
			} else {
				system.sendMessage(svi[od], 3, hash);
			}
			long startTime = System.currentTimeMillis();
			while (true) {
				if (pristiglo.containsKey(hash)) {
					Object temp = pristiglo.get(hash);
					pristiglo.remove(hash);
					return temp;
				}
				if ((System.currentTimeMillis() - startTime) > cekanje) {
					krepali.add(svi[od]);
					break;
				}
				system.yield();
			}

		}

		if (!krepali.contains(new Integer(svi[odBackup]))) {
			if (svi[odBackup] == system.getProcessId()) {
				pristiglo.put(hash, bunker.get(hash));
			} else {
				system.sendMessage(svi[odBackup], 3, hash);
			}
			long startTime = System.currentTimeMillis();
			while (true) {
				if (pristiglo.containsKey(hash)) {
					Object temp = pristiglo.get(hash);
					pristiglo.remove(hash);
					return temp;
				}
				if ((System.currentTimeMillis() - startTime) > cekanje) {
					krepali.add(svi[odBackup]);
					break;
				}
				system.yield();
			}

		}

		System.out.println("vracam -1");
		return -1;
	}

	@Override
	public void put(int hash, Object object) {
		//System.out.println("Put: " + hash);
		int za = hash % svi.length;
		int backup = backup(za);

		// System.out.println("------------------------>"+svi.length+" "+za+" "+backup);

		if (!krepali.contains(new Integer(svi[za]))) {// ///////////////////salje
														// se prvom ako nije u
														// listi mrtvih
			if (svi[za] == system.getProcessId()) {
				messageReceived(system.getProcessId(), 1, new Paket(object,
						hash));
			} else {
				system.sendMessage(svi[za], 1, new Paket(object, hash));
			}

			long startTime = System.currentTimeMillis();
			while (poslataPorukaJeUpisana == false) {
				if ((System.currentTimeMillis() - startTime) > cekanje) {
					krepali.add(svi[za]);
					break;
				}
				system.yield();
			}

			poslataPorukaJeUpisana = false;
		}

		if (!krepali.contains(new Integer(svi[backup]))) {// /////////////////salje
															// se drugom ako
															// nije u listi
															// mrtvih
			if (svi[backup] == system.getProcessId()) {
				messageReceived(system.getProcessId(), 1, new Paket(object,
						hash));
			} else {
				system.sendMessage(svi[backup], 1, new Paket(object, hash));
			}

			long startTime = System.currentTimeMillis();
			while (poslataPorukaJeUpisana == false) {
				if ((System.currentTimeMillis() - startTime) > cekanje) {
					krepali.add(svi[backup]);
					break;
				}
				system.yield();
			}

			poslataPorukaJeUpisana = false;
		}

	}

	@Override
	public void messageReceived(int from, int type, Object message) {
		// System.out.println("message: "+message);

		if (type == 1) {
			bunker.put(((Paket) message).hash, ((Paket) message).object);
			if (from == system.getProcessId()) {
				poslataPorukaJeUpisana = true;
			} else {
				system.sendMessage(from, 2, "PORUKA UPISANA");
			}
			//System.out.println("pokuka je upisana " + from);
		} else if (type == 2) {
			poslataPorukaJeUpisana = true;
		} else if (type == 3) {
			system.sendMessage(from, 4, new Paket(
					bunker.get((Integer) message), (Integer) message));
		} else if (type == 4) {
			pristiglo.put(((Paket) message).hash, ((Paket) message).object);
		}

	}

	@Override
	public void initialize() {
		svi = new int[system.getProcessNeighbourhood().length + 1];
		svi[0] = system.getProcessId();
		for (int i = 0; i < system.getProcessNeighbourhood().length; i++) {
			svi[i + 1] = system.getProcessNeighbourhood()[i];
		}
		Arrays.sort(svi);

	}

	public int backup(int za) {
		if (za == svi.length - 1) {
			return 0;
		} else {
			return (za + 1);
		}
	}
}

class Paket {

	public Paket(Object object, int hash) {
		this.object = object;
		this.hash = hash;
	}

	public Object object;
	public int hash;
}