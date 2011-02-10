package core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.tasks.Task;

import core.impl.ConcurrentTestSystemImpl;
import core.impl.NamedThreadFactory;

public class ConcurrentTester {

	public static boolean testTasks(Task... tasks) {
		ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("workers"));
		
		ConcurrentTestSystemImpl first = runSingle(executor, tasks);
		
		long sumTasks = 0;
		long sumSteps = 0;
		
		int n = 2000;
		
		for(int i = 0;i < n;i++) {
			System.gc();
			ConcurrentTestSystemImpl system = runSingle(executor, tasks);
			if (!system.equalFinalState(first)) {
				System.out.println("Different final state found");
				System.out.println("State 1 : ");
				system.printFinalState();
				System.out.println("State 2 : ");
				first.printFinalState();
				return false;
			}
			sumTasks += system.getStartedTasks();
			sumSteps += system.getSteps();
		}
		System.out.println("Test passed");
		System.out.println("Average steps : " + (((double)sumSteps)/n) + ", average tasks " + (((double)sumTasks)/n));
		return true;
	}

	private static ConcurrentTestSystemImpl runSingle(ExecutorService executor,
			Task... tasks) {
		ConcurrentTestSystemImpl system = new ConcurrentTestSystemImpl(executor);
		system.startTasks(tasks);
		return system;
	}
	
	
}
