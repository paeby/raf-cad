package kids.dist.examples.twomanconsensus;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.examples.consensus.ConsensusProblemInstance;

public class TwoManConsensusTester {
	
	public static void testTwoManConsensus(Class<? extends TwoManConsensus> solutionClass) {
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(0), solutionClass, new CliqueDistNetwork.Factory(2), 400))
			return;
		System.out.println("All good!");
	}
}
