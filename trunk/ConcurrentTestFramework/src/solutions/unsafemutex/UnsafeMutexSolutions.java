package solutions.unsafemutex;

import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.unsafemutex.UnsafeMutex;
import examples.unsafemutex.UnsafeMutexTester;
import examples.unsafequeue.UnsafeQueue;

public class UnsafeMutexSolutions {
	public final static class DummyUnsafeMutex implements UnsafeMutex {
		@Override
		public void lock() {}
		
		@Override
		public void unlock() {}
	}
	
	public final static class WorkingDummyUnsafeMutex implements UnsafeMutex {
		private final ReentrantLock lock = new ReentrantLock();
		
		@Override
		public void lock() {
			lock.lock();
		}
		
		@Override
		public void unlock() {
			lock.unlock();
		}
	}
	
	public final static class CasUnsafeMutex implements UnsafeMutex {
		private final AtomicReference<Thread> threadHoldingTheLock = new AtomicReference<Thread>(null);
		private final LinkedList<Thread> waitingThreads = new LinkedList<Thread>();
		private final AtomicBoolean workingWithWaitingList = new AtomicBoolean(false);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public void lock() {
			while (true) {
				try {
					while (!workingWithWaitingList.compareAndSet(false, true))
						Thread.yield();
					
					if (threadHoldingTheLock.compareAndSet(null, Thread.currentThread()))
						return;
					
					waitingThreads.addLast(Thread.currentThread());
				} finally {
					workingWithWaitingList.set(false);
				}
				
				unsafe.park(false, 0l);
			}
		}
		
		@Override
		public void unlock() {
			if (!threadHoldingTheLock.compareAndSet(Thread.currentThread(), null))
				throw new IllegalStateException("Unlock called by a thread not in posession of the lock");
			
			Thread waitingThread = null;
			try {
				while (!workingWithWaitingList.compareAndSet(false, true)) {
					Thread.yield();
				}
				if (!waitingThreads.isEmpty()) {
					waitingThread = waitingThreads.removeFirst();
				}
			} finally {
				workingWithWaitingList.set(false);
			}
			
			if (waitingThread != null)
				unsafe.unpark(waitingThread);
		}
	}
	
	public final static class FairMutex implements UnsafeMutex {
		final AtomicBoolean workingWithTheQueue = new AtomicBoolean(false);
		final LinkedList<Thread> waitingList = new LinkedList<Thread>();
		final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public void lock() {
			while (!workingWithTheQueue.compareAndSet(false, true))
				Thread.yield();
			waitingList.addLast(Thread.currentThread());
			while (waitingList.getFirst() != Thread.currentThread()) {
				workingWithTheQueue.set(false);
				
				unsafe.park(false, 0);
				
				while (!workingWithTheQueue.compareAndSet(false, true))
					Thread.yield();
			}
			workingWithTheQueue.set(false);
		}
		
		@Override
		public void unlock() {
			while (!workingWithTheQueue.compareAndSet(false, true))
				Thread.yield();
			
			waitingList.removeFirst();
			Thread otherThread = waitingList.isEmpty() ? null : waitingList.getFirst();
			
			workingWithTheQueue.set(false);
			if (otherThread != null) {
				while (otherThread.getState() != Thread.State.WAITING)
					Thread.yield();
				unsafe.unpark(otherThread);
			}
		}
	}
	
	public final static class BakeryBusyWaitMutex implements UnsafeMutex {
		private final AtomicLong ticketDispenser = new AtomicLong(Long.MIN_VALUE);
		private final AtomicLong currentCustomer = new AtomicLong(Long.MIN_VALUE);
		
		@Override
		public void lock() {
			long myTicket = ticketDispenser.getAndIncrement();
			while (currentCustomer.get() != myTicket)
				Thread.yield();
		}
		
		@Override
		public void unlock() {
			currentCustomer.incrementAndGet();
		}
	}
	
	public final static class BakeryParkingMutex implements UnsafeMutex {
		private static final int ARRAY_SIZE = 1000;
		private final AtomicLong ticketDispenser = new AtomicLong(0);
		private final AtomicLong currentCustomer = new AtomicLong(0);
		private final AtomicReferenceArray<Thread> customersInLine = new AtomicReferenceArray<Thread>(ARRAY_SIZE);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		private final AtomicBoolean unlockingInProgress = new AtomicBoolean();
		
		@Override
		public void lock() {
			long myTicket = ticketDispenser.getAndIncrement();
			customersInLine.set((int) (myTicket % ARRAY_SIZE), Thread.currentThread());
			while (unlockingInProgress.get() != false)
				Thread.yield();
			while (currentCustomer.get() != myTicket) {
				unsafe.park(false, 0l);
			}
			customersInLine.set((int) (myTicket % ARRAY_SIZE), null);
		}
		
		@Override
		public void unlock() {
			long myTicket = currentCustomer.get();
			Thread nextToWakeUp = null;
			unlockingInProgress.set(true);
			if (ticketDispenser.get() > myTicket + 1) {
				while ((nextToWakeUp = customersInLine.get((int) ((myTicket + 1) % ARRAY_SIZE))) == null)
					Thread.yield();
				while (nextToWakeUp.getState() != Thread.State.WAITING) {
					unlockingInProgress.set(false);
					Thread.yield();
					unlockingInProgress.set(true);
				}
			}
			currentCustomer.incrementAndGet();
			unlockingInProgress.set(false);
			if (nextToWakeUp != null)
				unsafe.unpark(nextToWakeUp);
		}
	}
	
	public final static class BakeryComplicatedMutex implements UnsafeMutex {
		final TreeSet<ThreadInfo> threadInfos = new TreeSet<ThreadInfo>();
		final AtomicBoolean workingWithSet = new AtomicBoolean();
		final AtomicLong ticket = new AtomicLong(Long.MIN_VALUE);
		final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public void lock() {
			while (!workingWithSet.compareAndSet(false, true))
				Thread.yield();
			
			long myTicket = ticket.incrementAndGet();
			ThreadInfo myThreadInfo = new ThreadInfo(myTicket);
			
			threadInfos.add(myThreadInfo);
			
			while (true) {
				final ThreadInfo smallestTicketThreadInfo = threadInfos.first();
				workingWithSet.set(false);
				
				if (smallestTicketThreadInfo == myThreadInfo)
					return;
				
				Thread.yield();
				// unsafe.park(false, 0l);
				
				while (!workingWithSet.compareAndSet(false, true))
					Thread.yield();
			}
		}
		
		@Override
		public void unlock() {
			while (!workingWithSet.compareAndSet(false, true))
				Thread.yield();
			ThreadInfo myInfo = threadInfos.first();
			if (myInfo.thread != Thread.currentThread())
				throw new IllegalStateException("Ne izbacujem sebe!");
			threadInfos.remove(myInfo);
			
			// if (!threadInfos.isEmpty()) {
			// ThreadInfo secondBest = threadInfos.first();
			// while (secondBest.thread.getState() != Thread.State.WAITING)
			// Thread.yield();
			// unsafe.unpark(secondBest);
			// }
			workingWithSet.set(false);
		}
		
		static class ThreadInfo implements Comparable<ThreadInfo> {
			final Thread thread;
			final long ticket;
			
			public ThreadInfo(long ticket) {
				this.thread = Thread.currentThread();
				this.ticket = ticket;
			}
			
			@Override
			public String toString() {
				return "" + ticket;
			}
			
			@Override
			public int compareTo(ThreadInfo o) {
				if (ticket < o.ticket)
					return -1;
				if (ticket > o.ticket)
					return 1;
				else
					return 0;
			}
		}
	}
	
	public final static class FairerMutex implements UnsafeMutex {
		final Unsafe unsafe = UnsafeHelper.getUnsafe();
		final WaitFreeQueue<Thread> waitingThreads = new WaitFreeQueue<Thread>();
		final AtomicBoolean acquiringTheSecondElemInTheQueueLock = new AtomicBoolean(false);
		
		@Override
		public void lock() {
			waitingThreads.enqueue(Thread.currentThread());
			while (waitingThreads.peek() != Thread.currentThread()) {
				unsafe.park(false, 0);
			}
		}
		
		@Override
		public void unlock() {
			Thread thisThread = waitingThreads.dequeue();
			if (thisThread != Thread.currentThread())
				throw new IllegalStateException();
			
			Thread otherThread = waitingThreads.peek();
			
			if (otherThread == null)
				return;
			
			while (otherThread.getState() != Thread.State.WAITING) {
				if (otherThread.getState() == Thread.State.TERMINATED)
					throw new IllegalStateException("Terminated!");
				Thread.yield();
			}
			unsafe.unpark(otherThread);
			return;
		}
	}
	
	public static class WaitFreeQueue<T> {
		final AtomicReference<Node> head = new AtomicReference<Node>(null);
		final AtomicReference<Node> tail = new AtomicReference<Node>(null);
		
		public WaitFreeQueue() {
			Node node = new Node(null);
			head.set(node);
			tail.set(node);
		}
		
		public void enqueue(T value) {
			Node node = new Node(value);
			while (true) {
				Node t = tail.get();
				Node s = t.next.get();
				if (t == tail.get()) {
					if (s == null) {
						if (t.next.compareAndSet(s, node)) {
							tail.compareAndSet(t, node);
							return;
						}
					} else {
						tail.compareAndSet(t, s);
					}
				}
			}
		}
		
		public T peek() {
			while (true) {
				Node h = head.get();
				Node t = tail.get();
				Node first = h.next.get();
				if (h == head.get()) {
					if (h == t) {
						if (first == null)
							return null;
						else
							tail.compareAndSet(t, first);
					} else {
						T value = first.value.get();
						if (value != null)
							return value;
						else
							head.compareAndSet(h, first);
					}
				}
			}
		}
		
		public T dequeue() {
			while (true) {
				Node h = head.get();
				Node t = tail.get();
				Node first = h.next.get();
				if (h == head.get()) {
					if (h == t) {
						if (first == null)
							return null;
						else
							tail.compareAndSet(t, first);
					} else if (head.compareAndSet(h, first)) {
						T value = first.value.get();
						if (value != null) {
							first.value.set(null);
							return value;
						}
					}
				}
			}
		}
		
		class Node {
			public AtomicReference<T> value;
			public AtomicReference<Node> next;
			
			public Node(T value) {
				this.value = new AtomicReference<T>(value);
				this.next = new AtomicReference<Node>(null);
			}
		}
	}
	
	public static class UnsafeQueueWrapper implements UnsafeQueue {
		public final WaitFreeQueue<Integer> queue = new WaitFreeQueue<Integer>();
		
		public UnsafeQueueWrapper() {}
		
		@Override
		public int remove() {
			Integer value = queue.dequeue();
			return value == null ? -1 : value;
		}
		
		@Override
		public void put(int value) {
			queue.enqueue(value);
		}
	}
	
	public static void main(String[] args) {
		// final AtomicBoolean done = new AtomicBoolean();
		// final UnsafeQueueWrapper unsafeQueue = new UnsafeQueueWrapper();
		// new Thread() {
		// public void run() {
		// while (!done.get())
		// unsafeQueue.queue.peek();
		// };
		// }.start();
		//
		// for (int i = 0; i < 100; i++) {
		// UnsafeQueueTester.testUnsafeQueue(unsafeQueue);
		// }
		// done.set(true);
		UnsafeMutexTester.testUnsafeMutex(new BakeryParkingMutex());
	}
	
}
