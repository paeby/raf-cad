package examples.addition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class AdditionProblemInstance implements ProblemInstance<Addition> {
	private final int processes;
	private final int operations;
	
	public AdditionProblemInstance(int processes, int operations) {
		super();
		this.processes = processes;
		this.operations = operations;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Addition solution) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final ArrayList<Integer> results = new ArrayList<Integer>();
		results.add(0);
		
		for (int i = 0; i < processes; i++) {
			final int curId = i;
			final ProcessInfo callerInfo = new ProcessInfo(i, processes);
			managedSystem.startTaskConcurrently(new Task() {
				
				@Override
				public void execute(ConcurrentSystem system) {
					for (int i = 0; i < operations; i++) {
						int toAdd = (curId+1) + processes * i;
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling add "+ toAdd);
						int result = solution.addAndGet(toAdd, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " add finished with " + result);						
						results.add(result);
					}
				}
			});
		}
		
		managedSystem.startSimAndWaitToFinish();
		
		Collections.sort(results);
		boolean[] taken = new boolean[processes*(1+operations) + 1];
		for(int i = 0;i<results.size()-1;i++)
		{
			int diff = results.get(i+1)-results.get(i);
			if (diff == 0) {
				correct.set(false);
				managedSystem.addLogLine("\t\t\tnot incremented for " + results.get(i));			
			} else if (diff >= taken.length) {
				correct.set(false);
				managedSystem.addLogLine("\t\t\tdifference too large " + diff);
			} else if (taken[diff]) {
				correct.set(false);
				managedSystem.addLogLine("\t\t\ttwice incremented by " + diff);			
			} else taken[diff] = true;
		}
		if (!correct.get())
			managedSystem.addLogLine("full list : " + results);
		return correct.get();
	}
	
	@Override
	public String toString() {
		return "AdditionProblemInstance[processes=" + processes + ", total operations=" + (processes*operations)+ "]";
	}

}
