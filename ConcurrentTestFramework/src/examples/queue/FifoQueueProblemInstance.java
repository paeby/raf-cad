package examples.queue;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class FifoQueueProblemInstance implements ProblemInstance<FifoQueue> {
	private final int howManyOperations, howManyProcesses;
	private final boolean addThenRemove;
	
	public FifoQueueProblemInstance(int processes, int operations, boolean addThenRemove) {
		this.howManyProcesses = processes;
		this.howManyOperations = operations;
		this.addThenRemove = addThenRemove;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final FifoQueue solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		final HashSet<Integer> insertedValues = new HashSet<Integer>(); 
		
		final AtomicInteger addsStarted = new AtomicInteger(1);
		final AtomicInteger addsDone = new AtomicInteger(1);
		final int[] addLower = new int[(howManyOperations + 1) * (howManyProcesses + 1)];
		final int[] addHigher = new int[(howManyOperations + 1) * (howManyProcesses + 1)];
		
		final AtomicInteger removesStarted = new AtomicInteger(1);
		final AtomicInteger removesDone = new AtomicInteger(1);
		
		
		final AtomicInteger addingDone = new AtomicInteger(howManyProcesses);
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex, howManyProcesses * 2);
			final int threadId = threadIndex;
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					int current = threadId;
					for (int i = 0; correct.get() && i < howManyOperations; i++) {
						addLower[current] = addsDone.get();
						addHigher[current] = Integer.MAX_VALUE;
						addsStarted.incrementAndGet();
						insertedValues.add(current);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " adding value " + current);
						solution.add(current, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " value " + current + " added.");
						addHigher[current] = addsStarted.get();
						addsDone.incrementAndGet();
						current += howManyProcesses;
					}
					addingDone.decrementAndGet();
				}
			});
		}
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex + howManyProcesses, howManyProcesses * 2);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					if (addThenRemove)
						while (addingDone.get() != 0)
							system.yield();
					while (correct.get() && removesDone.get() <= howManyProcesses * howManyOperations) {

						int lower = removesDone.get();
						removesStarted.incrementAndGet();
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " removing value");
						int value = solution.remove(managedSystem, callerInfo);
						if (value == -1) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " tried to remove from empty queue.");
							removesStarted.decrementAndGet();
							continue;
						}
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " removed value " + value);
						
						removesDone.incrementAndGet();
						int higher = removesStarted.get();
						
						if (!insertedValues.remove(value)) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** removed value " + value + " which hasn't been inserted");
							correct.set(false);
						} else if (higher < addLower[value] || addHigher[value] < lower) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** removed value " + value + " at wrong time, possible intervals don't intersect " 
									+ "[" + lower + "," + higher +"] and [" + addLower[value] + "," +  addHigher[value] +"]");
							correct.set(false);
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
