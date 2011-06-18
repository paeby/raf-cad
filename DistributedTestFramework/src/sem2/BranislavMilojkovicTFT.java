package sem2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;


public class BranislavMilojkovicTFT implements DistributedHashTable, InitiableSolution {

	private class PutRequest {
		private int requester;
		private int hash;
		private Object value;
		private int holder;

		public PutRequest(int requester, int hash, Object value, int holder) {
			this.requester = requester;
			this.hash = hash;
			this.value = value;
			this.holder = holder;
		}

		public int getRequester() {
			return requester;
		}

		public int getHash() {
			return hash;
		}

		public Object getValue() {
			return value;
		}

		public int getHolder() {
			return holder;
		}

		@Override
		public String toString() {
			return "[ requester: " + requester + " hash: " + hash + " value: " + value + " holder: " + holder + "]";
		}
	}
	
	private DistributedSystem system;
	
	private static final int SIZE = 256; //dizajner sistema je bio milostiv i dao nam ovu brojku
	private Object[] values; //<---- ht, bez d.
	private int[] clique; //nejbrhud sa sve sa mnom
	private int failed = -1; //ko je fejlovao, ako je fejlovao
	private Object receivedObject; //ovde pise get kad se desi
	private boolean responseArrived; //mocni sinhronizacioni mehanizam
	private boolean backupDone; //jos jedan mocni sinhronizacioni mehanizam
	private boolean[] waitForPut; //mamicuti konkurentsku, ovo mora da bude niz. jasta.
	private Map<Integer, List<PutRequest>> putRequestQueueMap; //mapa liste zahteva, yesyes, da se konkurentisu konkurenti pomocu nje
	
	@Override
	public void initialize() {
		values = new Object[SIZE];
		putRequestQueueMap = new HashMap<Integer, List<BranislavMilojkovicTFT.PutRequest>>();
		backupDone = true;
		waitForPut = new boolean[SIZE];
		
		clique = new int[system.getProcessNeighbourhood().length + 1];
		clique[0] = system.getProcessId();
		//skupi ih sve u tor
		for (int i = 1; i <= system.getProcessNeighbourhood().length; i++) {
			clique[i] = system.getProcessNeighbourhood()[i-1];
		}
		//usadi sebe
		for (int i = 0; i + 1 < clique.length; i++) {
			if (clique[i] > clique[i + 1]) {
				int t = clique[i];
				clique[i] = clique[i + 1];
				clique[i + 1] = t;
			} else {
				break;
			}
		}
		
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		switch (type) {
		case 1: //neko bi nesto
			sendToClique(from, 2, values[(Integer)message]);
			
			break;
		case 2: //stigao odgovor na molbu
			receivedObject = message;
			responseArrived = true;
			
			break;
		case 3: //neko bi da nesto nekome (tj. meni)
			PutRequest putRequest = (PutRequest)message;
			
			if (putRequestQueueMap.get(putRequest.getHash()) == null) {
				putRequestQueueMap.put(putRequest.getHash(), new LinkedList<PutRequest>());
			}
			
			if (waitForPut[putRequest.getHash()]) {
				putRequestQueueMap.get(putRequest.getHash()).add(putRequest);
			} else {
				waitForPut[putRequest.getHash()] = true;
				backupDone = false;
				//bekapbekap sistem restor jesss
				int backupHolder = getBackup(putRequest.getHolder());
				PutRequest backupRequest = new PutRequest(
						putRequest.getRequester(), putRequest.getHash(), putRequest.getValue(), backupHolder);
				
				sendToClique(backupHolder, 5, backupRequest);
				
				long timeSent = System.currentTimeMillis();
				
				while (!backupDone) {
					if (System.currentTimeMillis() > timeSent + 100) {
						break;
					}
					
					system.yield();
				}
				
				if (!backupDone) {
					failed = backupHolder;
				}
				
				//Naravno da ova dodela mora da bude ovde.
				//Zasto li sam mislio da moze da bude ispod for-a?
				//Sat vremena debaginga gore-dole ... boze moj.
				values[putRequest.getHash()] = putRequest.getValue();
				for (PutRequest reqInQueue : putRequestQueueMap.get(putRequest.getHash())) {
					sendToClique(reqInQueue.getRequester(), 4, null);
				}
				putRequestQueueMap.get(putRequest.getHash()).clear();
				
				sendToClique(putRequest.getRequester(), 4, null);
				
				//Slican komentar kao ovaj iznad vazi za ovu liniju.
				//Samo sto sam mislio da ona moze da bude *iznad* ove prethodne.
				//Koja je verovatnoca da imas dve takve greske u 7 linija?
				//Eeeee ali znam te, lisico, ovaj put mi je trebalo samo 10 minuta!
				waitForPut[putRequest.getHash()] = false;
			}
			break;
		case 4: //metno sam mu
			responseArrived = true;
			
			break;
		case 5: //neko bi da mu ja budem zaledjina.
			PutRequest toBackup = (PutRequest)message;
			values[toBackup.getHash()] = toBackup.getValue();
			sendToClique(from, 6, null);
			
			break;
		case 6: //uspeo backup
			backupDone = true;
			break;
		}
	}

	@Override
	public Object get(int hash) {
		int holder = getHolder(hash);
		
		while(true) {
			responseArrived = false;
			sendToClique(holder, 1, hash);
			
			long timeSent = System.currentTimeMillis();
			
			while (!responseArrived) {
				if (failed == holder || System.currentTimeMillis() > timeSent + 300) {
					break;
				}
				
				system.yield();
			}
			
			if (!responseArrived) { //crko :(
				failed = holder;
				holder = getBackup(holder);
				continue;
			}
			return receivedObject;
		}
		
	}
	
	@Override
	public void put(int hash, Object object) {
		int holder = getHolder(hash);
		
		while (true) {
			responseArrived = false;
			
			PutRequest putRequest = new PutRequest(system.getProcessId(), hash, object, holder);
			sendToClique(holder, 3, putRequest);
			
			long timeSent = System.currentTimeMillis();
			
			while (!responseArrived) {
				if (failed == holder || System.currentTimeMillis() > timeSent + 300) {
					break;
				}
				
				system.yield();
			}
			
			if (!responseArrived) {
				failed = holder;
				holder = getBackup(holder);
				continue;
			} else {
				break;
			}
		}
		
	}
	
	private void sendToClique(int destinationId, int type, Object message) {
		if (destinationId == system.getProcessId()) {
			messageReceived(destinationId, type, message);
		} else {
			system.sendMessage(destinationId, type, message);
		}
	}

	private int getBackup(int nodeId) { //eeee ... bitovske operacije
		return nodeId % 2 == 0 ? nodeId + 1 : nodeId - 1;
	}
	
	private int getHolder(int id) { //nadjem najblizeg u O(n) srammebilo
		int foundNode = -1;
		for (int i = 1; i < clique.length; i++) {
			if (clique[i] > id) {
				if (clique[i] - id < id - clique[i-1]) {
					foundNode = clique[i];
				} else {
					foundNode = clique[i-1];
				}
				
				break;
			}
		}
		
		if (foundNode == -1) {
			foundNode = clique[clique.length - 1];
		}
		if (foundNode == failed) {
			foundNode = getBackup(foundNode);
		}
		return foundNode;
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(BranislavMilojkovicTFT.class, true, false, true);
	}
}
