package examples.exchanger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExchangerProblemInstance {
	private final int howManyTimes;
	
	public ExchangerProblemInstance(int howManyTimes) {
		super();
		this.howManyTimes = howManyTimes;
	}
	
	public boolean testExchangerOnce(final Exchanger exchanger) {
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger waitForHowManyMoreThreads = new AtomicInteger(2);
		final AtomicReference<Exchanger> exchangerRef = new AtomicReference<Exchanger>(null);
		
		new Thread() {
			public void run() {
				try {
					int howManyTimesLeft = ExchangerProblemInstance.this.howManyTimes;
					while (howManyTimesLeft > 0) {
						Exchanger newExchanger = exchanger.getClass().newInstance();
						while (!exchangerRef.compareAndSet(null, newExchanger))
							Thread.yield();
						
						int myNum = howManyTimesLeft--;
						int result = exchangerRef.get().exchange(myNum);
						
						if (myNum != ExchangerProblemInstance.this.howManyTimes - result) {
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
		new Thread() {
			public void run() {
				try {
					int howManyTimesLeft = 0;
					while (howManyTimesLeft < ExchangerProblemInstance.this.howManyTimes) {
						while (exchangerRef.get() == null)
							Thread.yield();
						
						int myNum = howManyTimesLeft++;
						int result = exchangerRef.get().exchange(myNum);
						
						if (myNum != ExchangerProblemInstance.this.howManyTimes - result) {
							allOk.set(false);
							return;
						}
						
						exchangerRef.set(null);
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
