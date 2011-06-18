package sem2;

import java.util.Arrays;
import java.util.HashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
import kids.dist.seminarski2.DistributedHashTableTester;

public class MajaDHT implements DistributedHashTable, InitiableSolution {
	static final long bigTimeout = 500, smallTimeout = 100;
	static final int PUT = 0, PUT_FROM_MASTER = 1, GET = 2, REPLY = 3, FIND_CLOSEST = 4;

	DistributedSystem system;

	int[] t;
	int messageCounter, crashedId;

	HashMap<Integer, Object> map;
	HashMap<Integer, Message> replies;

	@Override
	public void initialize() {
		int[] ids = system.getProcessNeighbourhood();
		t = Arrays.copyOf(ids, ids.length + 1);
		t[ids.length] = system.getProcessId();

		messageCounter = 0;
		crashedId = -1;
		map = new HashMap<Integer, Object>();
		replies = new HashMap<Integer, Message>();
	}

	@Override
	public void messageReceived(int from, int type, Object message) {
		Message m = (Message) message;
		switch (type) {
		case REPLY:
			replies.put(m.messageId, m);
			break;
		case FIND_CLOSEST:
			int closest = getClosest(m.getHash());
			system.sendMessage(from, REPLY, new RedirectMessage(m.messageId, closest));
			break;
		case PUT:
			sendPutToSlave(m.getHash(), m.getObject());
			map.put(m.getHash(), m.getObject());
			system.sendMessage(from, REPLY, new RegularMessage(m.messageId, m.getHash(), null));
			break;
		case PUT_FROM_MASTER:
			map.put(m.getHash(), m.getObject());
			system.sendMessage(from, REPLY, new RegularMessage(m.messageId, m.getHash(), null));
			break;
		case GET:
			system.sendMessage(from, REPLY, new RegularMessage(m.messageId, m.getHash(), map.get(m.getHash())));
			break;
		}
	}
	private int findClosest(int hash) {
		int closest = getClosest(hash);
		boolean oneCrashed = false;
		while (true) {
			if (closest == system.getProcessId())
				return closest;
			int messageId = messageCounter++;
			sendMessageAndWaitForReply(closest, FIND_CLOSEST, new RegularMessage(messageId, hash, null), messageId,
					smallTimeout);
			Message reply = replies.remove(messageId);
			if (reply == null) {
				if (oneCrashed)
					return closest;
				closest = closest ^ 1;
				oneCrashed = true;
			} else if (reply.getRedirectTo() == closest)
				return closest;
			else
				closest = reply.getRedirectTo();
		}
	}
	private int getClosest(int hash) {
		int ret = 0;
		for (int i = 1; i < t.length; i++)
			if ((hash ^ t[i]) < (hash ^ t[ret]))
				ret = i;
		return t[ret];
	}

	private void sendMessageAndWaitForReply(int destination, int type, Object message, int messageId, long timeout) {
		if (destination == crashedId)
			return;
		system.sendMessage(destination, type, message);
		long start = System.currentTimeMillis();
		while (!replies.containsKey(messageId) && System.currentTimeMillis() < start + timeout)
			system.yield();
		if (!replies.containsKey(messageId)) {
			if (crashedId != -1 && crashedId != destination)
				System.err.println("Two crashed!!");
			crashedId = destination;
		}
	}

	private void sendPutToSlave(int hash, Object object) {
		int slave = system.getProcessId() ^ 1;
		int messageId = messageCounter++;
		sendMessageAndWaitForReply(slave, PUT_FROM_MASTER, new RegularMessage(messageId, hash, object), messageId,
				smallTimeout);
		replies.remove(messageId);
	}

	@Override
	public void put(int hash, Object object) {
		int closest = findClosest(hash);
		while (true) {
			if (closest == system.getProcessId()) {
				map.put(hash, object);
				sendPutToSlave(hash, object);
				break;
			}
			int messageId = messageCounter++;
			sendMessageAndWaitForReply(closest, PUT, new RegularMessage(messageId, hash, object), messageId, bigTimeout);
			if (replies.remove(messageId) == null)
				closest = closest ^ 1;
			else
				break;
		}
	}

	@Override
	public Object get(int hash) {
		int closest = findClosest(hash);
		while (true) {
			if (closest == system.getProcessId())
				return map.get(hash);
			int messageId = messageCounter++;
			sendMessageAndWaitForReply(closest, GET, new RegularMessage(messageId, hash, null), messageId, bigTimeout);
			Message reply = replies.remove(messageId);
			if (reply == null)
				closest = closest ^ 1;
			else
				return reply.getObject();
		}
	}

	static abstract class Message {
		final int messageId;
		Message(int messageId) {
			this.messageId = messageId;
		}
		boolean isRedirect() {
			return false;
		}
		int getRedirectTo() {
			return -1;
		}
		int getHash() {
			return -1;
		}
		Object getObject() {
			return null;
		}
		@Override
		public String toString() {
			return "(" + getHash() + " " + getObject() + ")";
		}
	}

	static class RedirectMessage extends Message {
		final int redirectTo;
		public RedirectMessage(int messageId, int redirectTo) {
			super(messageId);
			this.redirectTo = redirectTo;
		}
		@Override
		boolean isRedirect() {
			return true;
		}
		@Override
		int getRedirectTo() {
			return redirectTo;
		}
	}

	static class RegularMessage extends Message {
		final int hash;
		final Object object;
		public RegularMessage(int messageId, int hash, Object object) {
			super(messageId);
			this.hash = hash;
			this.object = object;
		}
		@Override
		int getHash() {
			return hash;
		}
		@Override
		Object getObject() {
			return object;
		}
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(MajaDHT.class, true, true, true);
	}
}
