package kids.dist.examples.mpigather;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;

public class MPIGatherTester {
	public static void testGather(Class<? extends MPIGather> solutionClass) {
		if (!ProblemTester.testProblem(new MPIGatherProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(128), 80))
			return;
		System.out.println("All good!");
	}
}
