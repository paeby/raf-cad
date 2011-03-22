package examples.blockingqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingQueueProblemInstance {
	final int numberOfThreads, insertionsPerThread;
	
	public BlockingQueueProblemInstance(int numberOfThreads, int insertionsPerThread) {
		super();
		this.numberOfThreads = numberOfThreads;
		this.insertionsPerThread = insertionsPerThread;
	}
	
	public boolean testBlockingQueueOnce(final BlockingQueue queue) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger threadsLeftToFinish = new AtomicInteger(numberOfThreads * 2);
		
		for (int i = 0; i < numberOfThreads; i++) {
			final int threadId = i;
			new Thread() {
				public void run() {
					for (int i = 0; i < insertionsPerThread; i++) {
						queue.put(i * numberOfThreads + threadId);
					}
					
					// all alright
					threadsLeftToFinish.decrementAndGet();
					synchronized (BlockingQueueProblemInstance.this) {
						BlockingQueueProblemInstance.this.notify();
					}
				};
			}.start();
		}
		for (int i = 0; i < numberOfThreads; i++) {
			new Thread() {
				public void run() {
					int[] stigaodo = new int[numberOfThreads];
					for (int i = 0; i < numberOfThreads; i++)
						stigaodo[i] = -1;
					
					for (int i = 0; i < insertionsPerThread; i++) {
						int value = queue.remove();
						if (stigaodo[value % numberOfThreads] >= value) {
							allOk.set(false);
							synchronized (BlockingQueueProblemInstance.this) {
								BlockingQueueProblemInstance.this.notify();
							}
							return;
						}
						stigaodo[value % numberOfThreads] = value;
					}
					
					// all alright
					threadsLeftToFinish.decrementAndGet();
					synchronized (BlockingQueueProblemInstance.this) {
						BlockingQueueProblemInstance.this.notify();
					}
				};
			}.start();
		}
		
		while (true) {
			synchronized (this) {
				try {
					if (!allOk.get() || threadsLeftToFinish.get() == 0)
						break;
					this.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return allOk.get();
	}
}
