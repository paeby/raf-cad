package kids.dist.solutions.seminarski2;

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
	private Object map[];
	private ArrayList<Object> putQueue[];
	private boolean waitingBackup[];
	private boolean backuped[];

	// client
	private int servers[] = { -1, -1 };
	private Object serverReply = null;
	private boolean serverReplied = false;

	@Override
	public Object get(int hash) {
		while (true) {
			long start = System.currentTimeMillis();
			int sendTo = (servers[0] != -1) ? servers[0] : servers[1];

			serverReplied = false;
			sendMessageAll(sendTo, GET_OBJECT, hash);
			while (!serverReplied && !isTimeouted(start))
				ds.yield();

			if (!serverReplied && isTimeouted(start)) {
				servers[0] = -1;
			} else {
				return serverReply;
			}
		}

	}

	@Override
	public void put(int hash, Object object) {
		while (true) {
			long start = System.currentTimeMillis();
			int sendTo = (servers[0] != -1) ? servers[0] : servers[1];

			serverReplied = false;
			sendMessageAll(sendTo, PUT_OBJECT, new MsgObject(hash, ds
					.getProcessId(), object));
			while (!serverReplied && !isTimeouted(start))
				ds.yield();

			if (!serverReplied && isTimeouted(start)) {
				servers[0] = -1;
			} else {
				return;
			}
		}
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
				int server = (servers[0] == ds.getProcessId() ? servers[1]
						: servers[0]);
				if (server != -1) {

					backuped[msg.hash] = false;

					sendMessageAll(server, PUT_BACKUP, msg);

					long start = System.currentTimeMillis();
					while (!backuped[msg.hash] && !isBackupTimeouted(start))
						ds.yield();

					if (!backuped[msg.hash] && isBackupTimeouted(start)) {
						if (server == servers[0])
							servers[0] = -1;
						else
							servers[1] = -1;
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
		if (ds.getProcessId() < ds.getProcessNeighbourhood()[1]) {
			map = new Object[256];
			putQueue = new ArrayList[256];
			for (int i = 0; i < 256; i++)
				putQueue[i] = new ArrayList<Object>();
			waitingBackup = new boolean[256];
			backuped = new boolean[256];
			servers[0] = ds.getProcessId();
		} else {
			servers[0] = ds.getProcessNeighbourhood()[1];
		}
		servers[1] = ds.getProcessNeighbourhood()[0];
		if (servers[1] < servers[0]) {
			int tmp = servers[1];
			servers[1] = servers[0];
			servers[0] = tmp;
		}
	}

	private void sendMessageAll(int to, int type, Object message) {
		if (to == ds.getProcessId()) {
			messageReceived(to, type, message);
		} else
			ds.sendMessage(to, type, message);
	}

	private boolean isTimeouted(long start) {
		return (System.currentTimeMillis() - start) > 600;
	}

	private boolean isBackupTimeouted(long start) {
		return (System.currentTimeMillis() - start) > 200;
	}

	private static class MsgObject {
		int hash;
		int sourceId;
		Object object;

		public MsgObject(int hash, int sourceId, Object object) {
			this.hash = hash;
			this.sourceId = sourceId;
			this.object = object;
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
		DistributedHashTableTester.testDHT(ReljaDHT.class, false, false, true);
	}

}
