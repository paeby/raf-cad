package solutions.copyonwritearray;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import sun.misc.Unsafe;
import useful.UnsafeHelper;
import examples.copyonwritearray.CopyOnWriteArray;
import examples.copyonwritearray.CopyOnWriteArrayTester;
import examples.unsafemutex.UnsafeMutex;
import examples.unsafereadwrite.UnsafeReadWriteLock;

public class CopyOnWriteArraySolutions {
	public static final class LockingCopyOnWriteArray implements CopyOnWriteArray {
		private final UnsafeReadWriteLock readWriteLock = new FairReadWriteLock();
		private final AtomicReference<int[]> array = new AtomicReference<int[]>(new int[100]);
		
		@Override
		public int[] get() {
			try {
				readWriteLock.lockRead();
				return array.get();
			} finally {
				readWriteLock.unlockRead();
			}
		}
		
		@Override
		public void set(int index, int value) {
			try {
				readWriteLock.lockWrite();
				int[] otherArray = Arrays.copyOf(array.get(), 100);
				otherArray[index] = value;
				array.set(otherArray);
			} finally {
				readWriteLock.unlockWrite();
			}
		}
	}
	
	public static class AtomicReferenceCopyOnWriteArray implements CopyOnWriteArray {
		private AtomicReference<int[]> reference = new AtomicReference<int[]>(new int[100]);
		private final FairMutex mutex = new FairMutex();
		
		@Override
		public int[] get() {
			return reference.get();
		}
		
		@Override
		public void set(int index, int value) {
			try {
				mutex.lock();
				int[] myCopy = new int[100];
				int[] nowArray = reference.get();
				for (int i = 0; i < 100; i++)
					myCopy[i] = nowArray[i];
			} finally {
				mutex.unlock();
			}
		}
		
	}
	
	public static class ChangeQueuingCopyOnWriteArray implements CopyOnWriteArray {
		private final FairMutex mutex = new FairMutex();
		private final AtomicReference<int[]> currentArray = new AtomicReference<int[]>(new int[100]);
		private final AtomicMarkableReference<int[]> otherArray = new AtomicMarkableReference<int[]>(new int[100], false);
		private final AtomicInteger indexOfChangeTicketDispenser = new AtomicInteger(0);
		
		@Override
		public int[] get() {
			return currentArray.get();
		}
		
		@Override
		public void set(int index, int value) {
			boolean unlocked = false;
			try {
				mutex.lock();
				int[] currentArray = this.currentArray.get();
				int[] newArray = this.otherArray.getReference();
				if (!otherArray.isMarked()) {
					otherArray.attemptMark(newArray, true);
					for (int i = 0; i < 100; i++)
						newArray[i] = currentArray[i];
					newArray[index] = value;
					mutex.unlock();
					mutex.lock();
					otherArray.compareAndSet(newArray, currentArray, true, false);
					this.currentArray.set(newArray);
					indexOfChangeTicketDispenser.incrementAndGet();
				} else {
					int ticket = indexOfChangeTicketDispenser.get();
					newArray[index] = value;
					mutex.unlock();
					unlocked = true;
					while (indexOfChangeTicketDispenser.get() == ticket)
						Thread.yield();
				}
			} finally {
				if (!unlocked)
					mutex.unlock();
			}
		}
		
	}
	
	public static class ChangeQueuingCopyOnWriteArray2 implements CopyOnWriteArray {
		private final static int ARRAY_SIZE = 1000;
		private final AtomicReferenceArray<Thread> threadsWaiting = new AtomicReferenceArray<Thread>(ARRAY_SIZE);
		private final AtomicReference<int[]> currentArray = new AtomicReference<int[]>(new int[100]);
		private final AtomicReference<int[]> currentlyEditingArray = new AtomicReference<int[]>(null);
		private final AtomicInteger threshold = new AtomicInteger(0);
		private final AtomicInteger head = new AtomicInteger(0);
		private final AtomicInteger tail = new AtomicInteger(0);
		private final AtomicBoolean gate = new AtomicBoolean(false);
		private final Unsafe unsafe = UnsafeHelper.getUnsafe();
		
		@Override
		public int[] get() {
			return currentArray.get();
		}
		
		@Override
		public void set(int index, int value) {
			int myTicket = head.getAndIncrement();
			if (!threadsWaiting.compareAndSet(myTicket % ARRAY_SIZE, null, Thread.currentThread()))
				throw new IllegalStateException("Thread overwrite");
			// / ja sam glavni kada threshold pokazuje na mene
			boolean bio = false;
			while (true) {
				int currentThreshold = threshold.get();
				while (gate.get())
					Thread.yield();
				if (myTicket == currentThreshold) {
					// treba da prepishem, vidim koji je novi threshold i
					// probudim ostale do novog thresholda
					// kada su svi zavrshili, postavljam novi threshold i budim
					// narednog ako postoji
					int[] current = currentArray.get();
					int[] editing = new int[100];
					for (int i = 0; i < 100; i++)
						editing[i] = current[i];
					if (!currentlyEditingArray.compareAndSet(null, editing))
						throw new IllegalStateException("Double editing");
					editing[index] = value;
					
					int newThreshold = head.get();
					
					Thread sleepingThread;
					for (int i = myTicket + 1; i < newThreshold; i++) {
						while ((sleepingThread = threadsWaiting.getAndSet(i % ARRAY_SIZE, null)) == null) {
							Thread.yield();
						}
						while (sleepingThread.getState() != Thread.State.WAITING)
							Thread.yield();
						unsafe.unpark(sleepingThread);
					}
					
					tail.incrementAndGet();
					while (tail.get() != newThreshold) {
						Thread.yield();
					}
					if (!currentArray.compareAndSet(current, currentlyEditingArray.getAndSet(null)))
						throw new IllegalStateException("Dvostruko prepisivanje");
					
					gate.set(true);
					if (!threshold.compareAndSet(currentThreshold, newThreshold))
						throw new IllegalStateException("Threshold se pomerio");
					if (head.get() > newThreshold) {
						while ((sleepingThread = threadsWaiting.getAndSet(newThreshold % ARRAY_SIZE, null)) == null)
							Thread.yield();
						while (sleepingThread.getState() != Thread.State.WAITING)
							Thread.yield();
						unsafe.unpark(sleepingThread);
					}
					gate.set(false);
					
					threadsWaiting.set(myTicket, null);
					return;
				} else {
					if (bio)
						throw new IllegalStateException();
					bio = true;
					unsafe.park(false, 0);
					currentThreshold = threshold.get();
					if (myTicket == currentThreshold)
						continue;
					// neko me je probudio da odradim svoj set. Odradicu ga i
					// cekati dok se threshold ne promeni
					currentlyEditingArray.get()[index] = value;
					tail.incrementAndGet();
					while (threshold.get() == currentThreshold)
						Thread.yield();
					return;
				}
			}
		}
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
	
	static class FairReadWriteLock implements UnsafeReadWriteLock {
		
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
		
	}
	
	public static void main(String[] args) {
		CopyOnWriteArrayTester.testCopyOnWriteArray(new AtomicReferenceCopyOnWriteArray());
	}
}
