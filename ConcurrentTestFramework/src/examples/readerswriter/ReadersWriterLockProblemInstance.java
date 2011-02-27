package examples.readerswriter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class ReadersWriterLockProblemInstance implements ProblemInstance<ReadersWriterLock> {
	
	final long testLengthInMilis;
	final int readerCount, writerCount;
	
	public ReadersWriterLockProblemInstance(long testLengthInMilis, int readerCount, int writerCount) {
		super();
		this.testLengthInMilis = testLengthInMilis;
		this.readerCount = readerCount;
		this.writerCount = writerCount;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final ReadersWriterLock solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		final AtomicInteger[] values = new AtomicInteger[10];
		for (int i = 0; i < values.length; i++)
			values[i] = new AtomicInteger(0);
		final AtomicInteger nextValue = new AtomicInteger(1);
		
		final AtomicLong totalWriteWait = new AtomicLong(0);
		final AtomicLong totalReadWait = new AtomicLong(0);
		final AtomicLong timesWriteWait = new AtomicLong(0);
		final AtomicLong timesReadWait = new AtomicLong(0);
		
		for (int processId = 0; processId < writerCount; processId++){
			final ProcessInfo callerInfo = new ProcessInfo(processId, readerCount + writerCount);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					final long startingtime = System.currentTimeMillis();
					while (correct.get() && System.currentTimeMillis() - startingtime < testLengthInMilis) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer acquiring lock");
						long waitStart = System.currentTimeMillis();
						solution.lockWrite(managedSystem, callerInfo);
						totalWriteWait.addAndGet(System.currentTimeMillis() - waitStart);
						timesWriteWait.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer acquired lock");
						
						managedSystem.yield();
						int value = nextValue.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writing values " + value);
						for (int i = 0; i < values.length; i++) {
							values[i].set(value);
							try {
								Thread.sleep(3);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							managedSystem.yield();
						}
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " done writing, releasing write lock");
						solution.unlockWrite(managedSystem, callerInfo);
					}
				}
			});
		}
		
		for (int processId = writerCount; processId < readerCount + writerCount; processId++) {
			final ProcessInfo callerInfo = new ProcessInfo(processId, readerCount + writerCount);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					final long startingtime = System.currentTimeMillis();
					while (correct.get() && System.currentTimeMillis() - startingtime < testLengthInMilis) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquiring lock");
						long waitStart = System.currentTimeMillis();
						solution.lockRead(managedSystem, callerInfo);
						totalReadWait.addAndGet(System.currentTimeMillis() - waitStart);
						timesReadWait.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquired lock");
						
						managedSystem.yield();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reading values ");
						for (int i = 1; i < values.length; i++) {
							try {
								Thread.sleep(3);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							managedSystem.yield();
							if (values[i - 1].get() != values[i].get()) {
								managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " found inconsistent array: " + Arrays.toString(values));
								correct.set(false);
							}
						}
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " done reading, releasing read lock");
						solution.unlockRead(managedSystem, callerInfo);
					}
				}
			});
		}
		
		managedSystem.waitToFinish();
		if (correct.get() && timesWriteWait.get() > 0 && timesReadWait.get() > 0) {
//			long writePercentage = totalWriteWait.get() * 100 / (writerCount * testLengthInMilis);
//			long readPercentage = totalReadWait.get() * 100 / (readerCount * testLengthInMilis);
//			System.out.println("Read waiting: " + (readPercentage > 100 ? 100 : readPercentage) + "%");
			System.out.println("Avg time to read: " + (totalReadWait.get() / timesReadWait.get()) + "ms");
//			System.out.println("Write waiting: " + (writePercentage > 100 ? 100 : writePercentage) + "%");
			System.out.println("Avg time to write: " + (totalWriteWait.get() / timesWriteWait.get()) + "ms");
			System.out.println(" --- ");
		}
		return correct.get();
	}
	
}
