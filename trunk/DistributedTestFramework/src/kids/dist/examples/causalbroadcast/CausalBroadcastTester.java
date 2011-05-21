package kids.dist.examples.causalbroadcast;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.core.network.SparseDistNetwork;

public class CausalBroadcastTester {
	public static void testBroadcastOnCliqueOnly(Class<? extends CausalBroadcast> solutionClass) {
		System.out.println("Testing causal broadcast on a clique..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(20), solutionClass, new CliqueDistNetwork.Factory(20), 100))
			return;
		System.out.println("All good!");
	}
	
	public static void testBroadcast(Class<? extends CausalBroadcast> solutionClass) {
		System.out.println("Testing causal broadcast on a clique..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(20), solutionClass, new CliqueDistNetwork.Factory(20), 100))
			return;
		System.out.println("Testing causal broadcast on a non-complete graph..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(20), solutionClass, new SparseDistNetwork.Factory(20, 25), 100))
			return;
		System.out.println("All good!");
	}
}
