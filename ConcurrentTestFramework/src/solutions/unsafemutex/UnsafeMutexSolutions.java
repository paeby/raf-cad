package solutions.unsafemutex;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.unsafemutex.UnsafeMutex;
import examples.unsafemutex.UnsafeMutexTester;

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
	
	public final static class QueuingMutex implements UnsafeMutex {
		final AtomicBoolean workingWithTheQueue = new AtomicBoolean(false);
		final LinkedList<Thread> queue = new LinkedList<Thread>();
		final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public void lock() {
			while (!workingWithTheQueue.compareAndSet(false, true))
				Thread.yield();
			queue.addLast(Thread.currentThread());
			while (queue.getFirst() != Thread.currentThread()) {
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
			
			queue.removeFirst();
			Thread otherThread = queue.isEmpty() ? null : queue.getFirst();
			
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
	

	
	public static void main(String[] args) {
		UnsafeMutexTester.testUnsafeMutex(new BakeryParkingMutex());
	}
	
}
