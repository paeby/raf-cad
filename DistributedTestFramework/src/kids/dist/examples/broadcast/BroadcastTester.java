package kids.dist.examples.broadcast;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.core.network.SparseDistNetwork;

public class BroadcastTester {
	public static void testBroadcast(Class<? extends Broadcast> solutionClass) {
		System.out.println("Testing broadcast on a clique..");
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(100), 40))
			return;
		System.out.println("Testing broadcast on a non-complete graph..");
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, new SparseDistNetwork.Factory(100, 25), 100))
			return;
		System.out.println("All good!");
	}
}
