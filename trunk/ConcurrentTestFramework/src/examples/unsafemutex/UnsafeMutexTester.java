package examples.unsafemutex;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class UnsafeMutexTester {
	private UnsafeMutexTester() {}
	
	public static void testUnsafeMutex(UnsafeMutex mutex) {
		UnsafeMutexTester tester = new UnsafeMutexTester();
		for (int i = 0; i < 2000; i++) {
			if (i % 100 == 0)
				System.out.print('.');
			if (!tester.testUnsafeMutexOnce(mutex)) {
				System.out.println("\nTest failed!");
				return;
			}
		}
		System.out.println("\nAll good!");
	}
	
	private boolean testUnsafeMutexOnce(final UnsafeMutex mutex) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger threadsLeftToFinish = new AtomicInteger(5);
		final AtomicBoolean shouldBeFalse = new AtomicBoolean(false);
		
		for (int i = 0; i < 5; i++) {
			new Thread() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						mutex.lock();
						if (!shouldBeFalse.compareAndSet(false, true)) {
							allOk.set(false);
							synchronized (UnsafeMutexTester.this) {
								UnsafeMutexTester.this.notify();
							}
							return;
						}
						Thread.yield();
						if (!shouldBeFalse.compareAndSet(true, false)) {
							allOk.set(false);
							synchronized (UnsafeMutexTester.this) {
								UnsafeMutexTester.this.notify();
							}
							return;
						}
						mutex.unlock();
					}
					// all alright
					threadsLeftToFinish.decrementAndGet();
					synchronized (UnsafeMutexTester.this) {
						UnsafeMutexTester.this.notify();
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
