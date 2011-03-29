package solutions.unsafereadwrite;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
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
		
		final static class FairMutex {
			final AtomicBoolean workingWithTheQueue = new AtomicBoolean(false);
			final LinkedList<Thread> waitingList = new LinkedList<Thread>();
			final Unsafe unsafe = UnsafeHelper.getUnsafe();
			
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
	}
	
	public static void main(String[] args) {
		UnsafeReadWriteLockTester.testUnsafeReadWriteLock(new FairReadWriteLock());
	}
}
