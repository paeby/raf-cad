package examples.consensus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class ConsensusProblemInstance implements ProblemInstance<Consensus> {
	private final int processes;

	public ConsensusProblemInstance(int processes) {
		this.processes = processes;
	}

	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Consensus solution) {	
		final AtomicBoolean correct = new AtomicBoolean(true);
		final AtomicInteger curAgreed = new AtomicInteger(-1);
		
		for (int processId = 0; processId < processes; processId++) {
			final ProcessInfo callerInfo = new ProcessInfo(processId, processes);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					int toPropose = callerInfo.getCurrentId() + 2;
					managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " proposing: " + toPropose);
					int agreed = solution.propose(toPropose, managedSystem, callerInfo);
					managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " agreed: " + agreed);
											
					if (agreed < 2 || agreed - 2 >= callerInfo.getTotalProcesses()) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " invalid agreed value " + agreed);
						correct.set(false);
					}
					
					if (curAgreed.get() <0) {
						curAgreed.set(agreed);
					} else if (agreed != curAgreed.get()) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " agreed on different value " + agreed + " != " + curAgreed.get());
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
		return "ConsensusProblemInstance[processes : " + processes + "]";
	}
}
