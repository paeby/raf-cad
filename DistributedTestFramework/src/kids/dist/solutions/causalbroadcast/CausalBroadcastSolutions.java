package kids.dist.solutions.causalbroadcast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import kids.dist.common.DistributedSystem;
import kids.dist.examples.causalbroadcast.CausalBroadcast;
import kids.dist.examples.causalbroadcast.CausalBroadcastTester;

public class CausalBroadcastSolutions {
	public static class IncorrectCausalBroadcast implements CausalBroadcast {
		DistributedSystem system;
		List<Object> receivedMessages = new LinkedList<Object>();
		
		@Override
		public void broadcast(Object msg) {
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, msg);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			receivedMessages.add(message);
		}
		
		@Override
		public List<Object> getReceivedMessages() {
			return receivedMessages;
		}
	}
	
	/**
	 * http://www2.cs.uidaho.edu/~krings/CS449/Notes.F05/449-05-19.pdf
	 * @author Bocete
	 *
	 */
	public static class ThreeWayHandshakeCausalBroadcast implements CausalBroadcast {
		DistributedSystem system;
		List<Object> receivedMessages = new LinkedList<Object>();
		
		LinkedList<Message> messagesQueue = new LinkedList<Message>();
		int waitForHowManyToSendMePriority;
		int maxPriority;
		int myMsgIndex = 0;
		
		@Override
		public void broadcast(Object msg) {
			int myId = system.getProcessId();
			maxPriority = messagesQueue.isEmpty() ? 0 : messagesQueue.getLast().priority + 1;
			int msgIndex = myMsgIndex++;
			
			waitForHowManyToSendMePriority = system.getProcessNeighbourhood().length;
			Object[] packed = new Object[] { msg, myId, msgIndex };
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, packed);
			while (waitForHowManyToSendMePriority > 0)
				system.yield();
			
			maxPriority++;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 2, new Message(msg, maxPriority, true, myId, msgIndex));
			receivedMessages.add(msg);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				Object[] packed = (Object[]) message;
				// zahtevanje id-a.
				int priority = messagesQueue.isEmpty() ? 0 : messagesQueue.getLast().priority + 1;
				messagesQueue.add(new Message(packed[0], priority, false, (Integer) packed[1], (Integer) packed[2]));
				system.sendMessage(from, 1, priority);
			} else if (type == 1) {
				// primio sam id od nekoga
				maxPriority = Math.max(maxPriority, (Integer) message);
				waitForHowManyToSendMePriority--;
			} else if (type == 2) {
				
				// izbacivanje ove iste poruke sa starim prioritetom iz queue-a
				ListIterator<Message> queueIterator = messagesQueue.listIterator();
				Message receivedMessage = (Message) message;
				while (true) {
					if (!queueIterator.hasNext())
						throw new IllegalStateException("Poruka nije pronadjena!");
					Message msgInQueue = queueIterator.next();
					if (msgInQueue.fromId == receivedMessage.fromId && msgInQueue.fromIndex == receivedMessage.fromIndex) {
						queueIterator.remove();
						while (queueIterator.hasNext() && msgInQueue.priority < receivedMessage.priority)
							msgInQueue = queueIterator.next();
						if (!queueIterator.hasNext())
							queueIterator.add(receivedMessage);
						else {
							queueIterator.previous();
							queueIterator.add(receivedMessage);
						}
						break;
					}
				}
				
				while (!messagesQueue.isEmpty() && messagesQueue.getFirst().deliverable) {
					receivedMessages.add(messagesQueue.removeFirst().msg);
				}
			}
		}
		
		@Override
		public List<Object> getReceivedMessages() {
			return receivedMessages;
		}
		
		private static class Message implements Comparable<Message> {
			public final Object msg;
			public final int priority;
			public final boolean deliverable;
			public final int fromId;
			public final int fromIndex;
			
			public Message(Object msg, int priority, boolean deliverable, int fromId, int fromIndex) {
				super();
				this.msg = msg;
				this.priority = priority;
				this.deliverable = deliverable;
				this.fromId = fromId;
				this.fromIndex = fromIndex;
			}
			
			@Override
			public int compareTo(Message o) {
				if (this.priority < o.priority)
					return -1;
				if (this.priority > o.priority)
					return 1;
				return 0;
			}
			
		}
	}
	
	public static class VectorClockCausalBroadcast implements CausalBroadcast {
		DistributedSystem system;
		final TreeMap<Integer, Integer> myClockVector = new TreeMap<Integer, Integer>();
		final Queue<PendingQueueItem> pending = new LinkedList<PendingQueueItem>();
		final List<Object> commitedMessages = new ArrayList<Object>();
		
		boolean vectorGreaterOrEqual(TreeMap<Integer, Integer> v1, TreeMap<Integer, Integer> v2) {
			for (Entry<Integer, Integer> entry : v2.entrySet())
				if (!v1.containsKey(entry.getKey()) || v1.get(entry.getKey()) < entry.getValue())
					return false;
			return true;
		}
		
		void increment(int id) {
			Integer oldV = myClockVector.get(id);
			if (oldV == null)
				myClockVector.put(id, 1);
			else
				myClockVector.put(id, oldV + 1);
		}
		
		@Override
		public void broadcast(Object msg) {
			commitedMessages.add(msg);
			Object myVectorClone = myClockVector.clone();
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, new Object[] {
						myVectorClone,
						msg });
			increment(system.getProcessId());
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void messageReceived(int from, int type, Object message) {
			Object[] array = (Object[]) message;
			pending.add(new PendingQueueItem(from, ((TreeMap<Integer, Integer>) array[0]), array[1]));
			commitCommitablePendingQueueItems();
		}
		
		/*
		 * ovo je ovako rešeno da bi uvek commit-ovao prvu poruku koju mogu da
		 * commit-ujem. ako pronađem neku poruku spremnu za commit, ponovo
		 * počinjem iteriranje ispočetka jer se možda neka poruka koju sam
		 * primio ranije otvorila za commit. Ovo ne mora ovako da se reši, samo
		 * je malo više fair na neki način.
		 */
		void commitCommitablePendingQueueItems() {
			restart: while (true) {
				Iterator<PendingQueueItem> queueItemIterator = pending.iterator();
				while (queueItemIterator.hasNext()) {
					PendingQueueItem queueItem = queueItemIterator.next();
					if (vectorGreaterOrEqual(myClockVector, queueItem.vector)) {
						commitedMessages.add(queueItem.message);
						increment(queueItem.from);
						queueItemIterator.remove();
						continue restart;
					}
				}
				break;
			}
		}
		
		@Override
		public List<Object> getReceivedMessages() {
			return commitedMessages;
		}
		
		static class PendingQueueItem {
			final int from;
			final TreeMap<Integer, Integer> vector;
			final Object message;
			
			public PendingQueueItem(int from, TreeMap<Integer, Integer> vector, Object message) {
				super();
				this.from = from;
				this.vector = vector;
				this.message = message;
			}
		}
	}
	
	public static void main(String[] args) {
		CausalBroadcastTester.testBroadcastOnCliqueOnly(ThreeWayHandshakeCausalBroadcast.class);
	}
}
