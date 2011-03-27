package examples.condition;

import java.util.concurrent.atomic.AtomicInteger;

public class ConditionProblemInstance {
	private final int howManyTimes;
	private final int numOfThreads;
	
	public ConditionProblemInstance(int howManyTimes, int numOfThreads) {
		super();
		this.howManyTimes = howManyTimes;
		this.numOfThreads = numOfThreads;
	}
	
	public boolean testConditionOnce(final Condition masterCondition) {
		try {
			final AtomicInteger waitForHowManyMoreThreads = new AtomicInteger(numOfThreads);
			final AtomicInteger a = new AtomicInteger();
			final Condition readySetGo = masterCondition.getClass().newInstance();
			final Condition condition = masterCondition.getClass().newInstance();
			
			for (int i = 0; i < numOfThreads; i++) {
				new Thread() {
					
					public void run() {
						try {
							readySetGo.myWait();
							
							int howManyTimesLeft = ConditionProblemInstance.this.howManyTimes;
							while (howManyTimesLeft-- > 0) {
								condition.myWait();
								Thread.sleep(0, 1);
								
								int locala = a.get();
								Thread.yield();
								a.set(locala + 1);
								
								condition.myNotify();
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
			
			Thread.sleep(100);
			readySetGo.myNotifyAll();
			Thread.sleep(100);
			condition.myNotify();
			
			synchronized (waitForHowManyMoreThreads) {
				while (waitForHowManyMoreThreads.get() > 0) {
					try {
						waitForHowManyMoreThreads.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
			
			if (a.get() < howManyTimes * numOfThreads - 1) {
				System.out.println(a.get() + " but should be " + (howManyTimes * numOfThreads - 1));
				return false;
			} else
				return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
