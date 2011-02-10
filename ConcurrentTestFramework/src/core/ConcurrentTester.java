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
		
		for(int i = 0;i < 10000;i++) {
			ConcurrentTestSystemImpl system = runSingle(executor, tasks);
			if (!system.equalFinalState(first)) {
				System.out.println("Different final state found");
				System.out.println("State 1 : ");
				system.printFinalState();
				System.out.println("State 2 : ");
				first.printFinalState();
				return false;
			}
		}
		System.out.println("test passed");
		return true;
	}

	private static ConcurrentTestSystemImpl runSingle(ExecutorService executor,
			Task... tasks) {
		ConcurrentTestSystemImpl system = new ConcurrentTestSystemImpl(executor);
		system.startTasks(tasks);
		return system;
	}
	
	
}
