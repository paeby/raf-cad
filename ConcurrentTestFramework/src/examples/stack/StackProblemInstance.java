package examples.stack;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

/**
 * nije dobar test
 * @author Bocete
 *
 */
@Deprecated
public class StackProblemInstance implements ProblemInstance<Stack> {
	private final int howManyOperations, howManyProcesses;
	private final boolean addThenRemove;
	
	public StackProblemInstance(int processes, int operations, boolean addThenRemove) {
		this.howManyProcesses = processes;
		this.howManyOperations = operations;
		this.addThenRemove = addThenRemove;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Stack solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		final BitSet insertedValues = new BitSet(howManyProcesses * howManyOperations);
		final Map<Integer, BitSet> valuesInsertedBeforeThisOneStarted = new HashMap<Integer, BitSet>();
		final AtomicInteger addingDone = new AtomicInteger(howManyProcesses);
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex, howManyProcesses * 2);
			final int threadId = threadIndex;
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					try {
						int current = threadId;
						for (int i = 0; correct.get() && i < howManyOperations; i++) {
							valuesInsertedBeforeThisOneStarted.put(current, (BitSet) insertedValues.clone());
							
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " adding value " + current);
							solution.push(current, managedSystem, callerInfo);
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " value " + current + " added.");
							
							insertedValues.set(current);
							current += howManyProcesses;
						}
					} finally {
						addingDone.decrementAndGet();
					}
				}
			});
		}
		
		final AtomicInteger removesStarted = new AtomicInteger(0);
		final AtomicInteger removesRemaining = new AtomicInteger(howManyOperations * howManyProcesses);
		
		final BitSet readValues = new BitSet(howManyOperations * howManyProcesses);
		
		for (int threadIndex = 0; threadIndex < howManyProcesses; threadIndex++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadIndex + howManyProcesses, howManyProcesses * 2);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					if (addThenRemove)
						while (addingDone.get() != 0)
							system.yield();
					while (correct.get() && removesRemaining.get() > 0) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " removing value");
						int value = solution.poll(managedSystem, callerInfo);
						if (value == -1) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " tried to remove from an empty stack");
							removesStarted.decrementAndGet();
							system.yield();
							continue;
						}
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " removed value " + value);
						removesRemaining.decrementAndGet();
						readValues.set(value);
						
						if (!insertedValues.get(value)) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** removed value " + value + " which hasn't been inserted");
							correct.set(false);
						} else
							insertedValues.clear(value);
						
						BitSet intersection = (BitSet) readValues.clone();
						intersection.and(valuesInsertedBeforeThisOneStarted.get(value));
						if (!intersection.isEmpty()) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** removed value " + value + " at a wrong time");
							correct.set(false);
						}
						
					}
					if (correct.get()) {
						int value = solution.poll(managedSystem, callerInfo);
						if (value != -1) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** read value " + value + " that has not been inserted: " + value);
							correct.set(false);
						}
					}
				}
			});
		}
		managedSystem.startSimAndWaitToFinish();
		return correct.get();
	}
	
	@Override
	public String toString() {
		return "StackProblemInstance[processes : " + howManyProcesses + ", writes/reads per process : " + howManyOperations + ", allow simultaneous writes and reads : " + !addThenRemove + "]";
	}
}
