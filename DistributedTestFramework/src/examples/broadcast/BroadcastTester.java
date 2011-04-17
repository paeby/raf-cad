package examples.broadcast;

import core.impl.problem.ProblemTester;

public class BroadcastTester {
	public static void testBroadcast(Class<? extends Broadcast> solutionClass) {
		System.out.println("Testing broadcast on a clique..");
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, 100, 100, 100))
			return;
		System.out.println("Testing broadcast on a non-complete graph..");
		if (!ProblemTester.testProblem(new BroadcastProblemInstance(), solutionClass, 100, 25, 100))
			return;
		System.out.println("All good!");
	}
}
