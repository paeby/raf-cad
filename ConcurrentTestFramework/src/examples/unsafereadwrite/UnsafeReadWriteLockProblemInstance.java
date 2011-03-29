package examples.unsafereadwrite;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeReadWriteLockProblemInstance {
	
	private final int[] readerSteps;
	private final int[] writerSteps;
	
	public UnsafeReadWriteLockProblemInstance(int[] readerSteps, int[] writerSteps) {
		super();
		this.readerSteps = readerSteps;
		this.writerSteps = writerSteps;
	}
	
	public Integer testOnce(final UnsafeReadWriteLock unsafeReadWriteLock) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final AtomicInteger numberOfThreadsAlive = new AtomicInteger(readerSteps.length + writerSteps.length);
		final AtomicInteger maxReaders = new AtomicInteger();
		final AtomicInteger readersActive = new AtomicInteger();
		final AtomicInteger writersActive = new AtomicInteger();
		
		for (int i = 0; i < readerSteps.length; i++) {
			final int curStep = readerSteps[i];
			new Thread() {
				
				@Override
				public void run() {
					try {
						for (int i = 0; i < curStep && correct.get(); i++) {
							unsafeReadWriteLock.lockRead();
							
							int cur = readersActive.incrementAndGet();
							maxReaders.set(Math.max(maxReaders.get(), cur));
							
							if (writersActive.get() > 0)
								incorrect();
							Thread.yield();
							if (writersActive.get() > 0)
								incorrect();
							
							readersActive.decrementAndGet();
							
							unsafeReadWriteLock.unlockRead();							
						}
					} finally {
						synchronized (numberOfThreadsAlive) {
							numberOfThreadsAlive.decrementAndGet();
							numberOfThreadsAlive.notify();
						}
					}
				}
				
				private void incorrect() {
					correct.set(false);
					synchronized (numberOfThreadsAlive) {
						numberOfThreadsAlive.notify();
					}
				}
			}.start();
		}
		
		for (int i = 0; i < writerSteps.length; i++) {
			final int curStep = writerSteps[i];
			new Thread() {
				
				@Override
				public void run() {
					try {
						for (int i = 0; i < curStep; i++) {
							unsafeReadWriteLock.lockWrite();
							
							if (writersActive.incrementAndGet() > 1)
								incorrect();
							if (readersActive.get() > 0)
								incorrect();
							Thread.yield();
							if (writersActive.get() > 1)
								incorrect();
							if (readersActive.get() > 0)
								incorrect();
							
							writersActive.decrementAndGet();
							
							unsafeReadWriteLock.unlockWrite();
						}
					} finally {
						synchronized (numberOfThreadsAlive) {
							numberOfThreadsAlive.decrementAndGet();
							numberOfThreadsAlive.notify();
						}
					}
				}
				
				private void incorrect() {
					correct.set(false);
					synchronized (numberOfThreadsAlive) {
						numberOfThreadsAlive.notify();
					}
				}
			}.start();
		}
		
		synchronized (numberOfThreadsAlive) {
			while (correct.get() && numberOfThreadsAlive.get() > 0)
				try {
					numberOfThreadsAlive.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		}
		
		return correct.get() ? maxReaders.get() : null;
	}
}
