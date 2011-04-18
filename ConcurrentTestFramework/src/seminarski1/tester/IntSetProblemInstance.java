package seminarski1.tester;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import seminarski1.IntSet;

public class IntSetProblemInstance {
	
	private final static AtomicReference<ExecutorService> EXECUTOR = new AtomicReference<ExecutorService>(null);
	private final static Random RANDOM = new Random();
	private final TestIntegerSample[] samples;
	private final int numOfThreads, numOfStepsPerThread;
	private final boolean expectFalseReturns;
	
	public IntSetProblemInstance(int sampleSize, int numOfEditingThreads, int numberOfStepsPerThread, boolean expectFalseReturns) {
		this.samples = new TestIntegerSample[sampleSize];
		this.numOfThreads = numOfEditingThreads;
		this.numOfStepsPerThread = numberOfStepsPerThread;
		this.expectFalseReturns = expectFalseReturns;
		
		for (int i = 0; i < samples.length; i++) {
			createNewValue: while (true) {
				int newValue = RANDOM.nextInt();
				if (newValue == Integer.MIN_VALUE || newValue == Integer.MAX_VALUE)
					continue createNewValue;
				
				for (int j = 0; j < i; j++)
					if (samples[j].value == newValue)
						continue createNewValue;
				
				samples[i] = new TestIntegerSample(RANDOM.nextInt());
				break;
			}
		}
	}
	
	public String[] testAndGetErrorMessages(final IntSet intSet) {
		synchronized (EXECUTOR) {
			if (EXECUTOR.get() == null) {
				ExecutorService newExecutor = new ThreadPoolExecutor(20, Integer.MAX_VALUE, 100, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
				EXECUTOR.set(newExecutor);
			}
		}
		
		final AtomicInteger waitForHowManyThreadsToFinish = new AtomicInteger(numOfThreads);
		final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<String>();
		
		for (int i = 0; i < numOfThreads; i++) {
			EXECUTOR.get().execute(new Runnable() {
				@Override
				public void run() {
					try {
						for (int step = 0; step < numOfStepsPerThread; step++) {
							TestIntegerSample sample = samples[RANDOM.nextInt(samples.length)];
							synchronized (sample) {
								while (true) {
									// prvo, ako nas je neko probudio jer se
									// pojavila greška, kraj
									if (!errorMessages.isEmpty())
										return;
									if (sample.phaseChanging.get()) {
										// čekamo dok nam ne otvore da uđemo na
										// narednu fazu
										sample.wait();
										continue;
									} else if (!expectFalseReturns && sample.numOfThreadsExecuting.get() > 0) {
										// neko već radi u ovoj fazi, bolje da
										// čekamo na narednu
										sample.wait();
										continue;
									} else
										break;
								}
								sample.numOfThreadsExecuting.incrementAndGet();
							}
							
							SAMPLE_TESTING_PHASE myPhase = sample.currentPhase.get();
							if (myPhase == SAMPLE_TESTING_PHASE.INSERT || (myPhase == SAMPLE_TESTING_PHASE.RANDOM && RANDOM.nextBoolean())) {
								boolean succeeded = intSet.addInt(sample.value);
								boolean contains = intSet.contains(sample.value);
								if (!contains && myPhase != SAMPLE_TESTING_PHASE.RANDOM) {
									if (succeeded) {
										errorMessages.add("GREŠKA: contains vraća false nakon uspešnog ubacivanja ");
									} else {
										errorMessages.add("GREŠKA: ubacivanje nije uspelo iako element nije unutra ");
									}
									synchronized (sample) {
										sample.notifyAll();
									}
									return;
								}
							} else {
								boolean succeeded = intSet.removeInt(sample.value);
								boolean contains = intSet.contains(sample.value);
								if (contains && myPhase != SAMPLE_TESTING_PHASE.RANDOM) {
									if (succeeded) {
										errorMessages.add("GREŠKA: contains vraća true nakon uspešnog izbacivanja");
									} else {
										errorMessages.add("GREŠKA: izbacivanje nije uspelo iako je element unutra");
									}
									synchronized (sample) {
										sample.notifyAll();
									}
									return;
								}
							}
							
							// završio sam.
							synchronized (sample) {
								// predloži narednu fazu, time zabranivši nitima
								// da ulaze u ovu
								sample.phaseChanging.set(true);
								if (sample.numOfThreadsExecuting.get() == 1) {
									// ja sam poslednji, i otvaram narednu fazu
									if (!expectFalseReturns) {
										// ako ne dozvoljavam false, treba da
										// naredna vaza bude suprotna
										if (!sample.currentPhase.compareAndSet(myPhase, myPhase.getOpposite()))
											throw new RuntimeException("TesterException: phase not atomically changed");
									} else {
										// inache, sve je ok
										if (!sample.currentPhase.compareAndSet(myPhase, SAMPLE_TESTING_PHASE.getRandomPhase()))
											throw new RuntimeException("TesterException: phase not atomically changed");
									}
									sample.phaseChanging.set(false);
									sample.notifyAll();
								}
								sample.numOfThreadsExecuting.decrementAndGet();
							}
						}
					} catch (Throwable t) {
						errorMessages.add("Bacen je exception: " + t.getMessage());
						throw new RuntimeException(t);
					} finally {
						synchronized (waitForHowManyThreadsToFinish) {
							waitForHowManyThreadsToFinish.decrementAndGet();
							waitForHowManyThreadsToFinish.notify();
						}
					}
				}
			});
		}
		
		synchronized (waitForHowManyThreadsToFinish) {
			while (waitForHowManyThreadsToFinish.get() > 0 && errorMessages.isEmpty()) {
				try {
					waitForHowManyThreadsToFinish.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		// proveri da li je niz u redu, u odnosu na currentPhase u njima
		
		return errorMessages.isEmpty() ? null : errorMessages.toArray(new String[errorMessages.size()]);
	}
	
	private class TestIntegerSample {
		final int value;
		final AtomicInteger numOfThreadsExecuting = new AtomicInteger(0);
		final AtomicBoolean phaseChanging = new AtomicBoolean();
		final AtomicReference<SAMPLE_TESTING_PHASE> currentPhase = new AtomicReference<SAMPLE_TESTING_PHASE>(SAMPLE_TESTING_PHASE.INSERT);
		
		public TestIntegerSample(int value) {
			this.value = value;
		}
	}
	
	private enum SAMPLE_TESTING_PHASE {
		INSERT, REMOVE, RANDOM;
		
		public static SAMPLE_TESTING_PHASE getRandomPhase() {
			return SAMPLE_TESTING_PHASE.values()[IntSetProblemInstance.RANDOM.nextInt(3)];
		}
		
		public SAMPLE_TESTING_PHASE getOpposite() {
			if (this == INSERT)
				return REMOVE;
			else if (this == REMOVE)
				return INSERT;
			else
				throw new IllegalStateException("Random stanje nema suprotno");
		}
	}
	
	public static void shutdownExecutor() {
		synchronized (EXECUTOR) {
			if (EXECUTOR.get() != null) {
				EXECUTOR.getAndSet(null).shutdown();
			}
		}
	}
}
