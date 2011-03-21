package examples.blockingqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class BlockingQueueTester {
	private BlockingQueueTester() {}
	
	public static void testBlockingQueue(BlockingQueue queue) {
		BlockingQueueTester tester = new BlockingQueueTester();
		for (int i = 0; i < 200; i++) {
			// if (i % 10 == 0)
			System.out.print('.');
			if (!tester.testBlockingQueueOnce(queue)) {
				System.out.println("\nTest failed!");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
	
	private boolean testBlockingQueueOnce(final BlockingQueue queue) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger threadsLeftToFinish = new AtomicInteger(6);
		
		for (int i = 0; i < 3; i++) {
			final int threadId = i + 1;
			new Thread() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						queue.put(i + threadId * 100);
					}
					// all alright
					threadsLeftToFinish.decrementAndGet();
					synchronized (BlockingQueueTester.this) {
						BlockingQueueTester.this.notify();
					}
				};
			}.start();
		}
		for (int i = 0; i < 3; i++) {
			new Thread() {
				public void run() {
					int[] stigaodo = new int[3];
					for (int i = 0; i < 3; i++)
						stigaodo[i] = -1;
					
					for (int i = 0; i < 100; i++) {
						int value = queue.remove();
						if (stigaodo[value / 100 - 1] >= value) {
							allOk.set(false);
							synchronized (BlockingQueueTester.this) {
								BlockingQueueTester.this.notify();
							}
							return;
						}
						stigaodo[value / 100 - 1] = value;
					}
					
					// all alright
					threadsLeftToFinish.decrementAndGet();
					synchronized (BlockingQueueTester.this) {
						BlockingQueueTester.this.notify();
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
