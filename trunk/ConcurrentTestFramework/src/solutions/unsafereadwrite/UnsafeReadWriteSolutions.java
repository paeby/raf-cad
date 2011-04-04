package solutions.unsafereadwrite;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.unsafemutex.UnsafeMutex;
import examples.unsafereadwrite.UnsafeReadWriteLock;
import examples.unsafereadwrite.UnsafeReadWriteLockTester;

public class UnsafeReadWriteSolutions {
	public static class UnsafeNotWorking implements UnsafeReadWriteLock {
		
		@Override
		public void lockRead() {}
		
		@Override
		public void unlockRead() {}
		
		@Override
		public void lockWrite() {}
		
		@Override
		public void unlockWrite() {}
		
	}
	
	public static class UnsafeMutexReadWrite implements UnsafeReadWriteLock {
		private final AtomicInteger state = new AtomicInteger(0);
		
		@Override
		public void lockRead() {
			while (!state.compareAndSet(0, 1))
				Thread.yield();
		}
		
		@Override
		public void unlockRead() {
			state.set(0);
		}
		
		@Override
		public void lockWrite() {
			while (!state.compareAndSet(0, 1))
				Thread.yield();
		}
		
		@Override
		public void unlockWrite() {
			state.set(0);
		}
	}
	
	public static class ProperBusyWaitReadWrite implements UnsafeReadWriteLock {
		private final AtomicInteger state = new AtomicInteger(0);
		
		@Override
		public void lockRead() {
			int localState;
			while (true) {
				localState = state.get();
				if (localState >= 0 && state.compareAndSet(localState, localState + 1))
					break;
				Thread.yield();
			}
		}
		
		@Override
		public void unlockRead() {
			state.decrementAndGet();
		}
		
		@Override
		public void lockWrite() {
			while (!state.compareAndSet(0, -1))
				Thread.yield();
		}
		
		@Override
		public void unlockWrite() {
			state.set(0);
		}
	}
	
	public static class FairReadWriteLock implements UnsafeReadWriteLock {
		
		final FairMutex noWriters = new FairMutex();
		final FairMutex noReaders = new FairMutex();
		final AtomicInteger numberOfReaders = new AtomicInteger();
		
		@Override
		public void lockWrite() {
			noWriters.lock();
			noReaders.lock();
			noReaders.unlock();
		}
		
		@Override
		public void unlockWrite() {
			noWriters.unlock();
		}
		
		@Override
		public void lockRead() {
			noWriters.lock();
			if (numberOfReaders.getAndIncrement() == 0)
				noReaders.lock();
			noWriters.unlock();
		}
		
		@Override
		public void unlockRead() {
			if (numberOfReaders.decrementAndGet() == 0)
				noReaders.unlock();
		}
		
		final static class FairMutex implements UnsafeMutex {
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
	}
	
	public static void main(String[] args) {
		UnsafeReadWriteLockTester.testUnsafeReadWriteLock(new FairReadWriteLock());
	}
}
