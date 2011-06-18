package sem2;

import java.util.ArrayList;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class ReljaDHT implements DistributedHashTable, InitiableSolution {

	DistributedSystem ds;

	public static final int GET_OBJECT = 0;
	public static final int OBJECT_RECEIVED = 1;
	public static final int PUT_OBJECT = 2;
	public static final int PUT_FINISHED = 3;
	public static final int PUT_BACKUP = 5;
	public static final int PUT_BACKUP_FINISHED = 6;

	// server
	private Object map[] = new Object[256];
	private ArrayList<Object> putQueue[] = new ArrayList[256];
	private boolean waitingBackup[] = new boolean[256];
	private boolean backuped[] = new boolean[256];
	private int neigh[];

	// client
	private int servers[];
	private Object serverReply = null;
	private boolean serverReplied = false;
	private boolean failed[] = new boolean[256];

	@Override
	public Object get(int hash) {
		servers = getTwoNearest(hash);
		while (true) {
			long start = System.currentTimeMillis();
			int sendTo = (servers[0] != -1) ? servers[0] : servers[1];

			serverReplied = false;
			sendMessageAll(sendTo, GET_OBJECT, hash);
			while (!serverReplied && !isTimeouted(start))
				ds.yield();

			if (!serverReplied && isTimeouted(start)) {
				failed[servers[0]] = true;
				servers[0] = -1;
			} else {
				return serverReply;
			}
		}
	}

	@Override
	public void put(int hash, Object object) {
		servers = getTwoNearest(hash);
		while (true) {
			long start = System.currentTimeMillis();
			int sendTo = (servers[0] != -1) ? servers[0] : servers[1];
			int backupSrv = (servers[0] == sendTo) ? servers[1] : servers[0];

			serverReplied = false;
			sendMessageAll(sendTo, PUT_OBJECT, new MsgObject(hash, ds
					.getProcessId(), object, backupSrv));
			while (!serverReplied && !isTimeouted(start))
				ds.yield();

			if (!serverReplied && isTimeouted(start)) {
				failed[servers[0]] = true;
				servers[0] = -1;
			} else {
				return;
			}
		}
	}

	private int[] getTwoNearest(int id) {
		 int n1 = 256, n2 = 256, s1 = -1, s2 = -1;
		 for (int n : neigh) {
			 if ((n ^ id) < n2) {
				 n2 = n ^ id;
				 s2 = n;
				 if (n1 > n2) {
					 int t = n1;
					 n1 = n2;
					 n2 = t;
					 t = s1;
					 s1 = s2;
					 s2 = t;
				 }
			 }
		 }
		return new int[] { failed[s1] ? -1 : s1, failed[s2] ? -1 : s2 };
	}

	@Override
	public void messageReceived(int from, int type, Object message) {
		MsgObject msg;

		switch (type) {
		case GET_OBJECT:
			sendMessageAll(from, OBJECT_RECEIVED, map[(Integer) message]);
			break;

		case OBJECT_RECEIVED:
			serverReply = message;
			serverReplied = true;
			break;

		case PUT_OBJECT:
			msg = (MsgObject) message;
			if (waitingBackup[msg.hash]) {
				putQueue[msg.hash].add(msg);
			} else {
				waitingBackup[msg.hash] = true;
				if (msg.backupServer != -1) {

					backuped[msg.hash] = false;

					sendMessageAll(msg.backupServer, PUT_BACKUP, msg);

					long start = System.currentTimeMillis();
					while (!backuped[msg.hash] && !isBackupTimeouted(start))
						ds.yield();

					if (!backuped[msg.hash] && isBackupTimeouted(start)) {
						failed[msg.backupServer] = true;
					}
				}

				map[msg.hash] = msg.object;

				for (Object o : putQueue[msg.hash])
					sendMessageAll(((MsgObject) o).sourceId, PUT_FINISHED, null);
				putQueue[msg.hash].clear();
				sendMessageAll(msg.sourceId, PUT_FINISHED, null);
				waitingBackup[msg.hash] = false;
			}

			break;

		case PUT_FINISHED:
			serverReplied = true;
			break;

		case PUT_BACKUP:
			msg = (MsgObject) message;
			map[msg.hash] = msg.object;
			sendMessageAll(from, PUT_BACKUP_FINISHED, msg.hash);
			break;

		case PUT_BACKUP_FINISHED:
			backuped[(Integer) message] = true;
			break;

		default:
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() {
		for (int i = 0; i < 256; i++)
			putQueue[i] = new ArrayList<Object>();
		neigh = new int[ds.getProcessNeighbourhood().length + 1];
		neigh[0] = ds.getProcessId();
		for (int i = 0; i < ds.getProcessNeighbourhood().length; i++) {
			neigh[i + 1] = ds.getProcessNeighbourhood()[i];
		}
		for (int i = 0; i + 1 < neigh.length; i++) {
			if (neigh[i] > neigh[i + 1]) {
				int t = neigh[i];
				neigh[i] = neigh[i + 1];
				neigh[i + 1] = t;
			} else
				break;

		}
	}

	private void sendMessageAll(int to, int type, Object message) {
		if (to == ds.getProcessId()) {
			messageReceived(to, type, message);
		} else
			ds.sendMessage(to, type, message);
	}

	private boolean isTimeouted(long start) {
		return servers[0] != -1 && (System.currentTimeMillis() - start) > 300;
	}

	private boolean isBackupTimeouted(long start) {
		return (System.currentTimeMillis() - start) > 100;
	}

	private static class MsgObject {
		int hash;
		int sourceId;
		int backupServer;
		Object object;

		public MsgObject(int hash, int sourceId, Object object) {
			this.hash = hash;
			this.sourceId = sourceId;
			this.object = object;
		}

		public MsgObject(int hash, int sourceId, Object object, int backupServer) {
			this(hash, sourceId, object);
			this.backupServer = backupServer;
		}

		public int getBackupServer() {
			return backupServer;
		}

		public void setBackupServer(int backupServer) {
			this.backupServer = backupServer;
		}

		@Override
		public boolean equals(Object obj) {
			MsgObject othr = (MsgObject) obj;

			return hash == othr.hash && sourceId == othr.sourceId
					&& object.equals(othr.object);
		}

		@Override
		public String toString() {
			return "[hash: " + hash + "; source: " + sourceId + "; object: "
					+ object.toString() + "]";
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(ReljaDHT.class, true, false, true);
	}

}
