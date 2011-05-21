package kids.dist.examples.mpibcast;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.examples.broadcast.BroadcastProblemInstance;

public class MPIBroadcastTester {
	public static void testMPIBroadcast(Class<? extends MPIBroadcast> solutionClass) {
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(128), 40))
			return;
		System.out.println("All good!");
	}
}
