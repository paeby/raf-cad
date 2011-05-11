package kids.dist.solutions.mutex;

import java.util.Arrays;
import java.util.LinkedList;
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
	
	public static void main(String[] args) {
		MutexTester.testMutex(MutexTokenRing.class);
	}
}
