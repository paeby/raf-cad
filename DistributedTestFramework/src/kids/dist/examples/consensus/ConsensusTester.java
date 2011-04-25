package kids.dist.examples.consensus;

import kids.dist.core.impl.problem.ProblemTester;

public class ConsensusTester {
	public static void testConsensusInClique(Class<? extends Consensus> solutionClass) {
		System.out.println("Testing consensus in a clique..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(0), solutionClass, 20, 100, 400))
			return;
		System.out.println("All good!");
	}
	
	public static void testConsensusInNonclique(Class<? extends Consensus> solutionClass) {
		System.out.println("Testing consensus in a non-clique");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(0), solutionClass, 20, 25, 400))
			return;
		System.out.println("All good!");
	}
}
