package kids.dist.examples.mutex;

import kids.dist.core.impl.problem.ProblemTester;

public class MutexTester {
	public static void testMutex(Class<? extends Mutex> solutionClass) {
		System.out.println("Testing mutex in a clique..");
		if (!ProblemTester.testProblem(new MutexProblemInstance(), solutionClass, 20, 100, 40))
			return;
		System.out.println("All good!");
	}
}
