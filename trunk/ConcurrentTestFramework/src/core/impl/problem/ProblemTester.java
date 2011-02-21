package core.impl.problem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.problem.ProblemInstance;
import common.problem.Solution;

import core.impl.ConcurrentTestSystemImpl;
import core.impl.NamedThreadFactory;

public class ProblemTester {

	public static <S extends Solution> boolean testProblem(ProblemInstance<S> problem, S solution) {
		return testProblem(problem, solution, 2000);
	}
	
	public static <S extends Solution> boolean testProblem(ProblemInstance<S> problem, S solution, int n) {
		ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("workers"));
				
		long sumTasks = 0;
		long sumSteps = 0;
		
		for(int i = 0;i < n;i++) {
			System.gc();
			
			ConcurrentTestSystemImpl system = new ConcurrentTestSystemImpl(executor);
			boolean success = problem.execute(system, solution);
			
			if (!success) {
				System.out.println("Bad state found");
				System.out.println("State 1 : ");
				system.printFinalState();
				return false;
			}
			sumTasks += system.getStartedTasks();
			sumSteps += system.getSteps();
		}
		System.out.println("Test passed");
		System.out.println("Average steps : " + (((double)sumSteps)/n) + ", average tasks " + (((double)sumTasks)/n));
		return true;
	}
	

}
