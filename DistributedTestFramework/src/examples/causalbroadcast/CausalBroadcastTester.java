package examples.causalbroadcast;

import core.impl.problem.ProblemTester;

public class CausalBroadcastTester {
	public static void testBroadcastOnCliqueOnly(Class<? extends CausalBroadcast> solutionClass) {
		System.out.println("Testing causal broadcast on a clique..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(5), solutionClass, 5, 100, 100))
			return;
		System.out.println("All good!");
	}		
	
	public static void testBroadcast(Class<? extends CausalBroadcast> solutionClass) {
		System.out.println("Testing causal broadcast on a clique..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(5), solutionClass, 5, 100, 100))
			return;
		System.out.println("Testing causal broadcast on a non-complete graph..");
		if (!ProblemTester.testProblem(new CausalBroadcastProblemInstance(5), solutionClass, 100, 25, 100))
			return;
		System.out.println("All good!");
	}
}
