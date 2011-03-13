package examples.queue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class FifoQueueProblemInstance implements ProblemInstance<FifoQueue> {
	private final int howManyOperations, howManyProcesses;
	
	public FifoQueueProblemInstance(int processes, int operations) {
		this.howManyProcesses = processes;
		this.howManyOperations = operations;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final FifoQueue solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		final AtomicInteger[] readersAreHere = new AtomicInteger[howManyProcesses];
		for (int i = 0; i < howManyProcesses; i++)
			readersAreHere[i] = new AtomicInteger(-1);
		final AtomicInteger totalCountOfReads = new AtomicInteger(0);
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex, howManyProcesses * 2);
			final int threadId = threadIndex;
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					int current = threadId;
					for (int i = 0; correct.get() && i < howManyOperations; i++) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " adding value " + current);
						solution.add(current, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " value " + current + " added.");
						current += howManyProcesses;
					}
				}
			});
		}
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex + howManyProcesses, howManyProcesses * 2);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					while (correct.get() && totalCountOfReads.get() < howManyProcesses * howManyOperations) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " removing value");
						int value = solution.remove(managedSystem, callerInfo);
						if (value == -1) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " read from empty queue.");
							continue;
						}
						totalCountOfReads.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " read value " + value);
						
						AtomicInteger oldValueFromThatModus = readersAreHere[value % howManyProcesses];
						if (oldValueFromThatModus.get() >= value) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** read value " + value + " which has been inserted after " + oldValueFromThatModus.get());
							correct.set(false);
						} else {
							oldValueFromThatModus.set(value);
						}
					}
					int value = solution.remove(managedSystem, callerInfo);
					if (value != -1) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** read value " + value + " read value that has not been inserted: " + value);
						correct.set(false);
					}
				}
			});
		}
		managedSystem.startSimAndWaitToFinish();
		return correct.get();
	}
	
	@Override
	public String toString() {
		return "MutexProblemInstance[processes : " + howManyProcesses + ", writes/reads per process : " + howManyOperations + "]";
	}
}
