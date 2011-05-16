package kids.dist.examples.mpibcast;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.examples.broadcast.BroadcastProblemInstance;

public class MPIBroadcastTester {
	public static void testMPIBroadcast(Class<? extends MPIBroadcast> solutionClass) {
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, 128, 100, 40))
			return;
		System.out.println("All good!");
	}
}
