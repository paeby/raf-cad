package solutions.causalbroadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import common.DistributedSystem;

import examples.causalbroadcast.CausalBroadcast;
import examples.causalbroadcast.CausalBroadcastTester;

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
	
	public static class ThreeWayHandshakeCausalBroadcast implements CausalBroadcast {
		List<Object> receivedMessages = new LinkedList<Object>();
		DistributedSystem system;
		LinkedList<Message> messagesQueue = new LinkedList<Message>();
		int waitForHowManyToSendMePriority;
		int maxPriority = 0;
		
		@Override
		public void broadcast(Object msg) {
			waitForHowManyToSendMePriority = system.getProcessNeighbourhood().length;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, msg);
			while (waitForHowManyToSendMePriority > 0)
				system.handleMessages(this);
			maxPriority++;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 2, new Message(msg, maxPriority, true));
			receivedMessages.add(msg);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				// zahtevanje id-a.
				int priority = maxPriority; // messagesQueue.isEmpty() ? 0 :
											// messagesQueue.getLast().priority
											// + 1;
				messagesQueue.add(new Message(message, priority, false));
				system.sendMessage(from, 1, priority);
			} else if (type == 1) {
				// primio sam id od nekoga
				maxPriority = Math.max(maxPriority, (Integer) message);
				waitForHowManyToSendMePriority--;
			} else if (type == 2) {
				
				// izbacivanje ove iste poruke sa starim prioritetom iz queue-a
				Iterator<Message> queueIterator = messagesQueue.iterator();
				while (true) {
					if (!queueIterator.hasNext())
						throw new IllegalStateException("Poruka nije pronadjena!");
					Message msgInQueue = queueIterator.next();
					if (msgInQueue.msg.equals(((Message) message).msg)) {
						queueIterator.remove();
						break;
					}
				}
				
				messagesQueue.add((Message) message);
				Collections.sort(messagesQueue);
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
			
			public Message(Object msg, int priority, boolean deliverable) {
				super();
				this.msg = msg;
				this.priority = priority;
				this.deliverable = deliverable;
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
		final DistributedSystem system;
		final TreeMap<Integer, Integer> myClockVector;
		final Queue<PendingQueueItem> pending = new LinkedList<PendingQueueItem>();
		final List<Object> receivedMessages = new ArrayList<Object>();
		
		public VectorClockCausalBroadcast(DistributedSystem system) {
			this.system = system;
			this.myClockVector = new TreeMap<Integer, Integer>();
		}
		
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
			receivedMessages.add(msg);
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
			acceptAllPendingFromQueue();
		}
		
		void acceptAllPendingFromQueue() {
			boolean somethingChanged;
			do {
				somethingChanged = false;
				Iterator<PendingQueueItem> queueItemIterator = pending.iterator();
				while (queueItemIterator.hasNext()) {
					PendingQueueItem queueItem = queueItemIterator.next();
					if (vectorGreaterOrEqual(myClockVector, queueItem.vector)) {
						receivedMessages.add(queueItem.message);
						increment(queueItem.from);
						
						queueItemIterator.remove();
						somethingChanged = true;
					}
				}
			} while (somethingChanged);
		}
		
		@Override
		public List<Object> getReceivedMessages() {
			return receivedMessages;
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
		CausalBroadcastTester.testBroadcastOnCliqueOnly(VectorClockCausalBroadcast.class);
	}
}
