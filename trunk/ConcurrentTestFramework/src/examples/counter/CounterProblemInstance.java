package examples.counter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class CounterProblemInstance implements ProblemInstance<Counter> {
	
	final int[] countIncrements;
	
	public CounterProblemInstance(int... countIncrements) {
		this.countIncrements = countIncrements;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Counter solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final AtomicInteger started = new AtomicInteger();
		final AtomicInteger finished = new AtomicInteger();
		
		for (int i = 0; i < countIncrements.length; i++) {
			final int count = countIncrements[i];
			final ProcessInfo callerInfo = new ProcessInfo(i, countIncrements.length);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					for (int i = 0; i < count; i++) {
						started.incrementAndGet();
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling inc");
						solution.inc(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " inc finished");
						finished.incrementAndGet();
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling getValue");
						int before = finished.get();
						int cur = solution.getValue(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " getValue finished with " + cur);
						if (cur > started.get() || cur < before) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " getValue failed!!! not " + before + " < " + cur + " < " + started.get());
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
		return "CounterProblemInstance[increments by process : " + Arrays.toString(countIncrements) + "]";
	}
}
