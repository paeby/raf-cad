package kids.dist.examples.consensus;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.core.network.SparseDistNetwork;

public class ConsensusTester {
	public static void testConsensusInClique(Class<? extends Consensus> solutionClass) {
		System.out.println("Testing consensus in a clique..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(0), solutionClass, new CliqueDistNetwork.Factory(20), 400))
			return;
		System.out.println("All good!");
	}
	
	public static void testConsensusInNonclique(Class<? extends Consensus> solutionClass) {
		System.out.println("Testing consensus in a non-clique");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(0), solutionClass, new SparseDistNetwork.Factory(20, 25), 400))
			return;
		System.out.println("All good!");
	}
	
	public static void testConsensusWithCrashes(Class<? extends Consensus> solutionClass) {
		System.out.println("Testing consensus with crashes, long timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(2, 100), solutionClass, new CliqueDistNetwork.Factory(20), 200))
			return;
		System.out.println("Testing consensus with crashes, medium timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(2, 20), solutionClass, new CliqueDistNetwork.Factory(20), 200))
			return;
		System.out.println("Testing consensus with crashes, short timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(2, 2), solutionClass, new CliqueDistNetwork.Factory(20), 200))
			return;
		System.out.println("Testing consensus with crashes, immediate crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(2, 0), solutionClass, new CliqueDistNetwork.Factory(20), 200))
			return;
		System.out.println("All good!");
	}
}
