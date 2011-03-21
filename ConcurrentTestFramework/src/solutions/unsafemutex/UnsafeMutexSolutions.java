package solutions.unsafemutex;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.unsafemutex.UnsafeMutex;
import examples.unsafemutex.UnsafeMutexTester;

public class UnsafeMutexSolutions {
	final static class DummyUnsafeMutex implements UnsafeMutex {
		@Override
		public void lock() {}
		
		@Override
		public void unlock() {}
	}
	
	final static class WorkingDummyUnsafeMutex implements UnsafeMutex {
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
	
	final static class CasUnsafeMutex implements UnsafeMutex {
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
			
			while (!workingWithWaitingList.compareAndSet(false, true)) {
				Thread.yield();
			}
			Thread waitingThread = null;
			if (!waitingThreads.isEmpty()) {
				waitingThread = waitingThreads.removeFirst();
			}
			workingWithWaitingList.set(false);
			
			if (waitingThread != null)
				unsafe.unpark(waitingThread);
		}
	}
	
	public static void main(String[] args) {
		UnsafeMutexTester.testUnsafeMutex(new CasUnsafeMutex());
	}
}
