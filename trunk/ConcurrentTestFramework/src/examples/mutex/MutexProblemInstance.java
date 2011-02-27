package examples.mutex;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class MutexProblemInstance implements ProblemInstance<Mutex> {
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Mutex solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		final AtomicInteger counter = new AtomicInteger(0);
		final AtomicInteger shouldBeCounter = new AtomicInteger(0);
		
		for (int processId = 0; processId < 5; processId++) {
			final ProcessInfo callerInfo = new ProcessInfo(processId, 5);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					for (int i = 0; correct.get() && i < 10; i++) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " aquiring lock");
						solution.lock(system, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " lock acquired");
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " incrementing");
						int current = counter.get();
						managedSystem.yield();
						counter.set(current + 1);
						shouldBeCounter.incrementAndGet();
						
						if (counter.get() != shouldBeCounter.get()) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** Invalid increment!! counter = " + counter.get() + " instead of " + shouldBeCounter.get());
							correct.set(false);
						}
						
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " releasing lock");
						solution.unlock(system, callerInfo);
					}
				}
			});
		}
		
		managedSystem.waitToFinish();
		return correct.get();
	}
	
}
