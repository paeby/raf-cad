package kids.dist.examples.mpigather;

import kids.dist.core.impl.problem.ProblemTester;

public class MPIGatherTester {
	public static void testGather(Class<? extends MPIGather> solutionClass) {
		if (!ProblemTester.testProblem(new MPIGatherProblemInstance(), solutionClass, 4, 100, 40))
			return;
		System.out.println("All good!");
	}
}
