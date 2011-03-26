package examples.gatherer;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GathererProblemInstance {
	private final int howManyTimes;
	
	public GathererProblemInstance(int howManyTimes) {
		super();
		this.howManyTimes = howManyTimes;
	}
	
	public boolean testGathererOnce(final Gatherer gatherer) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger waitForHowManyMoreThreads = new AtomicInteger(5);
		final AtomicReference<Gatherer> gathererRef = new AtomicReference<Gatherer>(null);
		final AtomicInteger seed = new AtomicInteger();
		final Random random = new Random();
		
		final AtomicInteger waitingThreadCount = new AtomicInteger(0);
		for (int i = 0; i < 5; i++) {
			final int threadId = i;
			new Thread() {
				public void run() {
					try {
						int howManyTimesLeft = GathererProblemInstance.this.howManyTimes;
						Gatherer recentGatherer;
						while (allOk.get() && howManyTimesLeft-- > 0) {
							synchronized (waitingThreadCount) {
								int waiting = waitingThreadCount.incrementAndGet();
								if (waiting == 5) {
									recentGatherer = gatherer.getClass().newInstance();
									gathererRef.set(recentGatherer);
									seed.set(random.nextInt(256));
									
									waitingThreadCount.set(0);
									waitingThreadCount.notifyAll();
								} else {
									waitingThreadCount.wait();
									recentGatherer = gathererRef.get();
								}
							}
							
							Object[] gathered = recentGatherer.offer(threadId, (seed.get() * threadId) & 0xff);
							if (gathered == null) {
								allOk.set(false);
								throw new NullPointerException("Offer je vratio null");
							}
							
							if (gathered.length != 5) {
								allOk.set(false);
								throw new RuntimeException("Offer je vratio niz koji nije dužine 5");
							}
							
							Integer otherInt = (Integer) gathered[(threadId + 1) % 5];
							if (otherInt == null) {
								allOk.set(false);
								throw new RuntimeException("Element u nizu povratnom nizu je null");
							}
							if (otherInt.intValue() != ((seed.get() * ((threadId + 1) % 5)) & 0xff)) {
								allOk.set(false);
								return;
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						synchronized (waitForHowManyMoreThreads) {
							waitForHowManyMoreThreads.decrementAndGet();
							waitForHowManyMoreThreads.notify();
						}
					}
				};
			}.start();
		}
		synchronized (waitForHowManyMoreThreads) {
			while (allOk.get() && waitForHowManyMoreThreads.get() > 0) {
				try {
					waitForHowManyMoreThreads.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		return allOk.get();
	}
}
