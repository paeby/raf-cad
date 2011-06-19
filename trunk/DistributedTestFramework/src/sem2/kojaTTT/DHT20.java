package sem2.kojaTTT;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;


public class DHT20 implements InitiableSolution, DistributedHashTable {

	DistributedSystem distSis;
	Boolean cekam;
	int[] susedi;
	Object[] upamceni;
	Object vrati;
	int idUmrlog;
	
	private static int TIMEOUT = 50;
	
	// type: 0 trazim blizeg zahtev, 1 PUT objekta zahtev, 2 GET objekta zahtev, 3 odgovor
	
	@Override
	public void initialize() {
		susedi = distSis.getProcessNeighbourhood();
		upamceni = new Object[256];
		idUmrlog = -1;
	}
	
	@Override
	public Object get(int hash) {
		
		int idNajblizeg = nadjiNajblizeg(hash);
		if ((distSis.getProcessId() ^ hash) <= (idNajblizeg ^ hash)) {
			return upamceni[hash];
		}
		
		idNajblizeg = iterirajNajblizeg(idNajblizeg, hash);
		
		if (idNajblizeg == distSis.getProcessId()) {
			return upamceni[hash];
		}
		
		cekam = true;
		distSis.sendMessage(idNajblizeg, 2, hash);
		long vreme = System.currentTimeMillis();
		while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
			distSis.yield();
		}
		if (cekam) {
			idUmrlog = idNajblizeg;
			idNajblizeg = dualniNod(idNajblizeg);
			if (idNajblizeg == distSis.getProcessId()) {
				return upamceni[hash];
			}
			cekam = true;
			distSis.sendMessage(idNajblizeg, 2, hash);
			while (cekam) {
				distSis.yield();
			}
		}
		
		return vrati;
	}

	@Override
	public void put(int hash, Object object) {

		int idNajblizeg = nadjiNajblizeg(hash);
		if ((distSis.getProcessId() ^ hash) <= (idNajblizeg ^ hash)) {
			upamceni[hash] = object;
			idNajblizeg = dualniNod(distSis.getProcessId());
			cekam = true;
			distSis.sendMessage(idNajblizeg, 1, new Element(hash, object));
			long vreme = System.currentTimeMillis();
			while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
				distSis.yield();
			}
			if (cekam) idUmrlog = idNajblizeg;
			return;
		}
		
		idNajblizeg = iterirajNajblizeg(idNajblizeg, hash);
		
		if (idNajblizeg == distSis.getProcessId()) {
			upamceni[hash] = object;
			idNajblizeg = dualniNod(distSis.getProcessId());
			cekam = true;
			distSis.sendMessage(idNajblizeg, 1, new Element(hash, object));
			long vreme = System.currentTimeMillis();
			while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
				distSis.yield();
			}
			if (cekam) idUmrlog = idNajblizeg;
			return;
		}
		
		cekam = true;
		distSis.sendMessage(idNajblizeg, 1, new Element(hash, object));
		long vreme = System.currentTimeMillis();
		while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
			distSis.yield();
		}
		if (cekam) idUmrlog = idNajblizeg;
		
		idNajblizeg = dualniNod(idNajblizeg);
		
		if (idNajblizeg == distSis.getProcessId()) {
			upamceni[hash] = object;
			return;
		}
		
		if (idUmrlog != idNajblizeg) {
			cekam = true;
			distSis.sendMessage(idNajblizeg, 1, new Element(hash, object));
			vreme = System.currentTimeMillis();
			while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
				distSis.yield();
			}
			if (cekam) idUmrlog = idNajblizeg;
		}
		
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		if (type == 0) { // trazim blizeg zahtev
			int najblizi = nadjiNajblizeg((Integer)message);
			if ((distSis.getProcessId() ^ (Integer)message) > (najblizi ^ (Integer)message)) {
				distSis.sendMessage(from, 3, najblizi);
			} else {
				distSis.sendMessage(from, 3, -1);
			}
		} else
		if (type == 1) { // PUT objekta zahtev
			Element primljeni = (Element)message;
			upamceni[primljeni.getHash()] = primljeni.getObject();
			distSis.sendMessage(from, 3, null);
		} else
		if (type == 2) { // GET objekta zahtev
			distSis.sendMessage(from, 3, upamceni[(Integer)message]);
		} else
		if (type == 3) { // odgovor
			vrati = message;
			cekam = false;
		}
		
	}
	
	private int nadjiNajblizeg(int hash) {
		int najbliziXOR = hash ^ susedi[0];
		int nadjenNajblizi = 0;
		for (int i = 1; i < susedi.length; i++) {
			int trenutniXOR = hash ^ susedi[i];
			if (trenutniXOR < najbliziXOR) {
				najbliziXOR = trenutniXOR;
				nadjenNajblizi = i;
			}
		}
		return susedi[nadjenNajblizi];
	}
	
	private int iterirajNajblizeg(int idNajblizeg, int hash) {
		int poslati = idNajblizeg;
		int pronadjeniNajblizi = idNajblizeg;
		while (poslati != -1) {
			pronadjeniNajblizi = poslati;
			cekam = true;
			if (idUmrlog != poslati) {
				distSis.sendMessage(poslati, 0, hash);
				long vreme = System.currentTimeMillis();
				while (((System.currentTimeMillis() - vreme) < TIMEOUT) && cekam) {
					distSis.yield();
				}
				if (cekam) idUmrlog = poslati;
				else poslati = (Integer)vrati;
			}
			if (cekam) {
				poslati = dualniNod(poslati);
				if (poslati == distSis.getProcessId()) {
					return distSis.getProcessId();
				}
				pronadjeniNajblizi = poslati;
				distSis.sendMessage(poslati, 0, hash);
				while (cekam) {
					distSis.yield();
				}
				if (((Integer)vrati) == dualniNod(poslati)) {
					return pronadjeniNajblizi;
				}
				else poslati = (Integer)vrati;
			}
		}
		return pronadjeniNajblizi;
	}

	private int dualniNod(int nod) {
		if ((nod % 2) == 0) return (nod + 1);
		return (nod - 1);
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(DHT20.class, true, true, true);
	}
}