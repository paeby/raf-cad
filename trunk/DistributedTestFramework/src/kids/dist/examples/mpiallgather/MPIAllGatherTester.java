package kids.dist.examples.mpiallgather;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;

public class MPIAllGatherTester {
	public static void testAllGather(Class<? extends MPIAllGather> solutionClass) {
		if (!ProblemTester.testProblem(new MPIAllGatherProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(128), 80))
			return;
		System.out.println("All good!");
	}
}
