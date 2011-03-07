package examples.tuple;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class PairRegisterProblemInstance implements ProblemInstance<PairRegister> {
	final int timesToWriteAndOrRead[];
	
	public PairRegisterProblemInstance(int... timesToWriteOrReadPerThread) {
		super();
		this.timesToWriteAndOrRead = timesToWriteOrReadPerThread;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final PairRegister solution) {
		final AtomicBoolean ok = new AtomicBoolean(true);
		final AtomicInteger counter = new AtomicInteger(0);
		
		// writers
		for (int threadId = 0; threadId < timesToWriteAndOrRead.length; threadId++) {
			final ProcessInfo callerInfo = new ProcessInfo(threadId, timesToWriteAndOrRead.length * 2);
			final int tId = threadId;
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) {
					int toDo = timesToWriteAndOrRead[tId];
					while (ok.get() && toDo-- > 0) {
						int newValue = counter.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " writing values [" + newValue + ", " + newValue + "]");
						solution.write(newValue, newValue, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " done writing values [" + newValue + ", " + newValue + "]");
					}
				}
			});
		}
		
		// readers
		for (int threadId = 0; threadId < timesToWriteAndOrRead.length; threadId++) {
			final ProcessInfo callerInfo = new ProcessInfo(timesToWriteAndOrRead.length + threadId, timesToWriteAndOrRead.length * 2);
			final int tId = threadId;
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) {
					int toDo = timesToWriteAndOrRead[tId];
					
					int[] values;
					while (ok.get() && toDo-- > 0) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reading values");
						values = solution.read(managedSystem, callerInfo);
						// za slučaj da prosledi referencu koju će modifikovati,
						// ko zna šta im može pasti na pamet
						values = Arrays.copyOf(values, values.length);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " done reading values: " + Arrays.toString(values));
						
						if (values[0] != values[1]) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** inconsistent values found: " + Arrays.toString(values));
							ok.set(false);
						}
					}
				}
			});
		}
		managedSystem.startSimAndWaitToFinish();
		return ok.get();
	}
}
