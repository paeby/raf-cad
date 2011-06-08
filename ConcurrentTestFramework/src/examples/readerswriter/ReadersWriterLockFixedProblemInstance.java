package examples.readerswriter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class ReadersWriterLockFixedProblemInstance implements ProblemInstance<ReadersWriterLock> {
	
	private final int[] readerSteps;
	private final int[] writerSteps;
	
	public ReadersWriterLockFixedProblemInstance(int[] readerSteps, int[] writerSteps) {
		super();
		this.readerSteps = readerSteps;
		this.writerSteps = writerSteps;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final ReadersWriterLock solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final AtomicInteger maxReaders = new AtomicInteger();
		final AtomicInteger readersActive = new AtomicInteger();
		final AtomicInteger writersActive = new AtomicInteger();
		
		for (int i = 0; i < readerSteps.length; i++) {
			final int curStep = readerSteps[i];
			final ProcessInfo callerInfo = new ProcessInfo(i, readerSteps.length + writerSteps.length);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					for (int i = 0; i < curStep; i++) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquiring lock");
						solution.lockRead(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquired lock");
						
						int cur = readersActive.incrementAndGet();
						maxReaders.set(Math.max(maxReaders.get(), cur));
						if (writersActive.get() > 0)
							incorrect();
						system.yield();
						if (writersActive.get() > 0)
							incorrect();
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader realising lock");
						solution.unlockRead(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader realised lock");
						
						readersActive.decrementAndGet();
					}
				}
				
				@Override
				public String toString() {
					return "Reader";
				}
				
				private void incorrect() {
					managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " incorrect state: " + readersActive.get() + " readers and " + writersActive.get() + " writers");
					correct.set(false);
				}
			});
		}
		
		for (int i = 0; i < writerSteps.length; i++) {
			final int curStep = writerSteps[i];
			final ProcessInfo callerInfo = new ProcessInfo(readerSteps.length + i, readerSteps.length + writerSteps.length);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					for (int i = 0; i < curStep; i++) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer acquiring lock");
						solution.lockWrite(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer acquired lock");
						
						if (writersActive.incrementAndGet() > 1)
							incorrect();
						if (readersActive.get() > 0)
							incorrect();
						system.yield();
						if (writersActive.get() > 1)
							incorrect();
						if (readersActive.get() > 0)
							incorrect();
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer realising lock");
						solution.unlockWrite(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writer realised lock");
						writersActive.decrementAndGet();
					}
				}
				
				@Override
				public String toString() {
					return "Writer";
				}
				
				private void incorrect() {
					managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " incorrect state: " + readersActive.get() + " readers and " + writersActive.get() + " writers");
					correct.set(false);
				}
			});
		}
		managedSystem.startSimAndWaitToFinish();
		return correct.get();
	}
	
	@Override
	public String toString() {
		return "RWProblemInstance[readers : " + readerSteps.length + ", writers : " + writerSteps.length + "]";
	}
	
}
