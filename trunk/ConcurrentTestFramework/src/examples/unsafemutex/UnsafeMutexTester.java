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
	
	private static UnsafeMutex instantiateCopy(final UnsafeMutex masterMutex) {
		try {
			return masterMutex.getClass().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Ne mogu instantirati mutex bez default constructor-a!", e);
		}
	}
	
	private boolean testUnsafeMutexOnce(final UnsafeMutex masterMutex) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger threadsLeftToFinish = new AtomicInteger(5);
		final AtomicBoolean shouldBeFalse = new AtomicBoolean(false);
		final UnsafeMutex mutex = instantiateCopy(masterMutex);
		
		for (int i = 0; i < 5; i++) {
			new Thread() {
				public void run() {
					try {
						for (int i = 0; i < 100 && allOk.get(); i++) {
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
					} catch (RuntimeException ex) {
						allOk.set(false);
						throw ex;
					} finally {
						threadsLeftToFinish.decrementAndGet();
						synchronized (UnsafeMutexTester.this) {
							UnsafeMutexTester.this.notify();
						}
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
