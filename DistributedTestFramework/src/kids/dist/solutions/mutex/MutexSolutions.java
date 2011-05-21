package kids.dist.solutions.mutex;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.examples.mutex.Mutex;
import kids.dist.examples.mutex.MutexTester;

public class MutexSolutions {
	public static class MutexIncorrect implements Mutex {
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
		
		@Override
		public void lock() {}
		
		@Override
		public void unlock() {}
	}
	
	public static class MutexCheating implements Mutex {
		
		final static AtomicBoolean lock = new AtomicBoolean(false);
		
		DistributedSystem system;
		
		public MutexCheating() {
			lock.set(false);
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {}
		
		@Override
		public void lock() {
			while (!lock.compareAndSet(false, true))
				system.yield();
		}
		
		@Override
		public void unlock() {
			lock.set(false);
		}
	}
	
	public static class MutexCentralized implements Mutex, InitiableSolution {
		
		DistributedSystem system;
		LinkedList<Integer> requests;
		int myId, serverId;
		boolean lock;
		boolean iHaveTheLock = false;
		
		@Override
		public void initialize() {
			myId = system.getProcessId();
			serverId = system.getProcessNeighbourhood()[0];
			serverId = Math.min(myId, serverId);
			if (serverId == myId) {
				requests = new LinkedList<Integer>();
				lock = false;
			}
		}
		
		/*
		 * Tipovi poruka:
		 * 
		 * 0: zahtev za lock-om
		 * 1: confirm lock-a
		 * 2: release
		 */
		@Override
		public void messageReceived(int from, int type, Object message) {
			if (type == 0) {
				if (serverId != myId)
					throw new IllegalStateException();
				
				requests.addLast(from);
				if (requests.size() == 1) {
					lock = true;
					if (from != myId)
						system.sendMessage(from, 1, null);
					else
						messageReceived(from, 1, null);
				}
			} else if (type == 1) {
				iHaveTheLock = true;
			} else if (type == 2) {
				if (serverId != myId)
					throw new IllegalStateException();
				
				if (requests.size() == 0 || requests.getFirst() != from)
					throw new IllegalStateException("no no");
				requests.removeFirst();
				lock = false;
				if (requests.size() > 0) {
					lock = true;
					if (requests.getFirst() == myId)
						messageReceived(myId, 1, null);
					else
						system.sendMessage(requests.getFirst(), 1, null);
				}
			} else
				throw new IllegalArgumentException("Unknown type " + type);
		}
		
		@Override
		public void lock() {
			if (myId == serverId)
				messageReceived(myId, 0, null);
			else
				system.sendMessage(serverId, 0, null);
			
			while (!iHaveTheLock)
				system.yield();
		}
		
		@Override
		public void unlock() {
			iHaveTheLock = false;
			if (myId == serverId)
				messageReceived(myId, 2, null);
			else
				system.sendMessage(serverId, 2, null);
		}
	}
	
	public static class MutexTokenRandom implements Mutex, InitiableSolution {
		
		DistributedSystem system;
		Random random = new Random();
		boolean allYourTokenAreBelongToUs, waiting;
		
		@Override
		public void initialize() {
			allYourTokenAreBelongToUs = false;
			waiting = false;
			
			if (system.getProcessId() < system.getProcessNeighbourhood()[0])
				releaseAndSendToken();
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			allYourTokenAreBelongToUs = true;
			if (!waiting) {
				releaseAndSendToken();
			}
		}
		
		@Override
		public void lock() {
			waiting = true;
			while (!allYourTokenAreBelongToUs)
				system.yield();
		}
		
		@Override
		public void unlock() {
			releaseAndSendToken();
		}
		
		void releaseAndSendToken() {
			waiting = false;
			allYourTokenAreBelongToUs = false;
			int[] neighbourhood = system.getProcessNeighbourhood();
			system.sendMessage(neighbourhood[random.nextInt(neighbourhood.length)], 0, null);
		}
	}
	
	public static class MutexTokenRing implements Mutex, InitiableSolution {
		
		DistributedSystem system;
		int nextGuy;
		boolean allYourTokenAreBelongToUs, waiting;
		
		@Override
		public void initialize() {
			allYourTokenAreBelongToUs = false;
			waiting = false;
			
			int myId = system.getProcessId();
			int[] neighbourhood = system.getProcessNeighbourhood();
			nextGuy = -Arrays.binarySearch(neighbourhood, myId) - 1;
			if (nextGuy == neighbourhood.length)
				nextGuy = neighbourhood[0];
			else
				nextGuy = neighbourhood[nextGuy];
			
			if (myId > nextGuy)
				releaseAndSendToken();
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			allYourTokenAreBelongToUs = true;
			if (!waiting) {
				releaseAndSendToken();
			}
		}
		
		@Override
		public void lock() {
			waiting = true;
			while (!allYourTokenAreBelongToUs)
				system.yield();
		}
		
		@Override
		public void unlock() {
			releaseAndSendToken();
		}
		
		void releaseAndSendToken() {
			waiting = false;
			allYourTokenAreBelongToUs = false;
			system.sendMessage(nextGuy, 0, null);
		}
	}
	
	public static class RicartAgrawalaMutex implements Mutex {
		DistributedSystem system;
		int time = 0;
		int repliesPending;
		Timestamp myRequestTimestamp = null;
		Collection<Integer> requestsThatNeedToBeRepliedTo = new LinkedList<Integer>();
		
		@Override
		public void lock() {
			myRequestTimestamp = createTimestamp();
			int[] neighbourhood = system.getProcessNeighbourhood();
			repliesPending = neighbourhood.length;
			for (int neighbour : neighbourhood)
				system.sendMessage(neighbour, 0, myRequestTimestamp);
			
			while (repliesPending > 0)
				system.yield();
		}
		
		@Override
		public void unlock() {
			myRequestTimestamp = null;
			if (!requestsThatNeedToBeRepliedTo.isEmpty()) {
				Timestamp ts = createTimestamp();
				for (Integer neighbour : requestsThatNeedToBeRepliedTo)
					system.sendMessage(neighbour, 1, ts);
			}
			requestsThatNeedToBeRepliedTo.clear();
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			Timestamp msgTimestamp = (Timestamp) message;
			if (time <= msgTimestamp.time)
				time = msgTimestamp.time + 1;
			
			if (type == 0) {
				// request received
				if (myRequestTimestamp == null || msgTimestamp.compareTo(myRequestTimestamp) < 0)
					system.sendMessage(from, 1, createTimestamp());
				else
					requestsThatNeedToBeRepliedTo.add(from);
			} else if (type == 1) {
				repliesPending--;
			}
		}
		
		Timestamp createTimestamp() {
			return new Timestamp(system.getProcessId(), time++);
		}
		
		class Timestamp implements Comparable<Timestamp> {
			final int id, time;
			
			public Timestamp(int id, int time) {
				super();
				this.id = id;
				this.time = time;
			}
			
			@Override
			public int compareTo(Timestamp o) {
				if (time != o.time)
					return time - o.time;
				return id - o.id;
			}
			
		}
	}
	
	public static class RaymondMutex implements Mutex, InitiableSolution {
		DistributedSystem system;
		
		int tokenLocation;
		Queue<Integer> requests = new LinkedList<Integer>();
		boolean requested = false;
		
		@Override
		public void initialize() {
			int[] neighbourhood = system.getProcessNeighbourhood();
			int myId = system.getProcessId();
			
			if (myId < neighbourhood[0])
				tokenLocation = myId;
			else {
				int myIndexMinusOne = -Arrays.binarySearch(neighbourhood, myId) - 1 - 1;
				tokenLocation = neighbourhood[myIndexMinusOne / 2];
			}
		}
		
		@Override
		public void lock() {
			int myId = system.getProcessId();
			requests.add(myId);
			
			if (tokenLocation != myId) {
				requestToken();
			}
			
			while (tokenLocation != myId) {
				if (!requested)
					throw new RuntimeException(myId + " Chekam na token koji nisam ni trazhio");
				
				system.yield();
			}
		}
		
		@Override
		public void unlock() {
			if (requests.peek() != system.getProcessId())
				throw new RuntimeException(system.getProcessId() + " Skidam prvi request, a to nisam ja");
			requests.poll();
			if (!requests.isEmpty()) {
				tokenLocation = requests.poll();
				
				requested = !requests.isEmpty();
				system.sendMessage(tokenLocation, 1, requested);
			}
		}
		
		@Override
		public void messageReceived(int from, int type, Object message) {
			int myId = system.getProcessId();
			if (type == 0) {
				// primio zahtev
				if (tokenLocation == myId && requests.isEmpty()) {
					tokenLocation = from;
					requested = !requests.isEmpty();
					system.sendMessage(from, 1, requested);
				} else {
					requests.add(from);
					requestToken();
				}
			} else {
				if ((Boolean) message) {
					requests.add(from);
				}
				
				// dobio sam token
				requested = false;
				int requestor = requests.peek();
				
				if (requestor == myId) {
					tokenLocation = myId;
				} else {
					tokenLocation = requestor;
					requests.poll();
					requested = !requests.isEmpty();
					system.sendMessage(requestor, 1, requested);
				}
			}
		}
		
		void requestToken() {
			int myId = system.getProcessId();
			if (tokenLocation != myId && !requested) {
				requested = true;
				system.sendMessage(tokenLocation, 0, null);
			}
		}
	}
	
	public static void main(String[] args) {
		MutexTester.testMutex(MutexCheating.class);
		MutexTester.testMutex(MutexCentralized.class);
		MutexTester.testMutex(MutexTokenRandom.class);
		MutexTester.testMutex(MutexTokenRing.class);
		MutexTester.testMutex(RicartAgrawalaMutex.class);
		MutexTester.testMutex(RaymondMutex.class);
	}
}
