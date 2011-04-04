package examples.copyonwritearray;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CopyOnWriteArrayProblemInstance {
	private final ExecutorService executor = new ThreadPoolExecutor(20, Integer.MAX_VALUE, 100, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
	
	public boolean testOnce(final CopyOnWriteArray array, final int steps, final int numOfThreads) {
		final AtomicInteger numberOfThreadsDone = new AtomicInteger(numOfThreads * 2);
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final Random random = new Random();
		
		for (int i = 0; i < numOfThreads; i++) {
			final int threadId = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						int index = 0, myNumber = threadId + 1;
						while (numberOfThreadsDone.get() > numOfThreads && allOk.get()) {
							while (random.nextBoolean())
								Thread.sleep(10);
							array.set(index++, myNumber);
							if (index >= 100) {
								index %= 100;
								myNumber += numOfThreads;
							}
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} finally {
						synchronized (numberOfThreadsDone) {
							numberOfThreadsDone.decrementAndGet();
							numberOfThreadsDone.notify();
						}
					}
				}
			});
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						int[] numbers = new int[numOfThreads];
						boolean foundZero;
						int step = 0;
						while (step++ < steps && allOk.get()) {
							Arrays.fill(numbers, 0);
							foundZero = false;
							int[] values = array.get();
							for (int i = 0; i < values.length; i++) {
								if (values[i] == 0)
									foundZero = true;
								else {
									int lastIveReadForThisThread = numbers[(values[i] - 1) % numOfThreads];
									if (foundZero || (lastIveReadForThisThread > 0 && lastIveReadForThisThread < values[i])) {
										allOk.set(false);
										return;
									} else
										numbers[(values[i] - 1) % numOfThreads] = values[i];
								}
							}
						}
					} finally {
						synchronized (numberOfThreadsDone) {
							numberOfThreadsDone.decrementAndGet();
							numberOfThreadsDone.notify();
						}
					}
				}
			});
		}
		
		synchronized (numberOfThreadsDone) {
			while (numberOfThreadsDone.get() > 0 && allOk.get())
				try {
					numberOfThreadsDone.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		}
		
		return allOk.get();
	}
	
	public void shutdownExecutor() {
		executor.shutdown();
	}
}
