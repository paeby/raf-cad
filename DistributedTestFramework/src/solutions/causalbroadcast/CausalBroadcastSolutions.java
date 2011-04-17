package solutions.causalbroadcast;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
	
	public static class HistoryCausalBroadcast implements CausalBroadcast {
		List<Object> receivedMessages = new LinkedList<Object>();
		DistributedSystem system;
		LinkedList<Message> messagesQueue = new LinkedList<Message>();
		int waitForHowManyToSendMePriority;
		int maxPriority;
		
		@Override
		public void broadcast(Object msg) {
			waitForHowManyToSendMePriority = system.getProcessNeighbourhood().length;
			maxPriority = 0;
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 0, msg);
			while (waitForHowManyToSendMePriority > 0)
				system.handleMessages(this);
			for (int neighbour : system.getProcessNeighbourhood())
				system.sendMessage(neighbour, 2, new Message(msg, maxPriority, true));
			receivedMessages.add(msg);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				// zahtevanje id-a.
				int priority = messagesQueue.isEmpty() ? 0 : messagesQueue.getLast().priority + 1;
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
					if (msgInQueue.msg.equals(((Message)message).msg)) {
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
	
	public static void main(String[] args) {
		CausalBroadcastTester.testBroadcastOnCliqueOnly(HistoryCausalBroadcast.class);
	}
}
