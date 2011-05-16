package kids.dist.examples.mpiallgather;

import kids.dist.core.impl.problem.ProblemTester;

public class MPIAllGatherTester {
	public static void testAllGather(Class<? extends MPIAllGather> solutionClass) {
		if (!ProblemTester.testProblem(new MPIAllGatherProblemInstance(), solutionClass, 128, 100, 80))
			return;
		System.out.println("All good!");
	}
}
