package kids.dist.examples.mutex;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;

public class MutexTester {
	public static void testMutex(Class<? extends Mutex> solutionClass) {
		System.out.println("Testing mutex in a clique..");
		if (!ProblemTester.testProblem(new MutexProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(20), 20))
			return;
		System.out.println("All good!");
	}
	
	public static void testLamportMutex(Class<? extends Mutex> solutionClass) {
		System.out.println("Testing mutex in a clique with two nodes..");
		if (!ProblemTester.testProblem(new MutexProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(2), 20, true, false))
			return;
		System.out.println("Testing mutex in a clique..");
		if (!ProblemTester.testProblem(new MutexProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(20), 20, true, false))
			return;
		System.out.println("All good!");
	}
}
