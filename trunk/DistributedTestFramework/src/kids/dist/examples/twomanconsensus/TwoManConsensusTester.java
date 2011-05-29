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
	
	public static void testTwoManConsensusWithCrashes(Class<? extends TwoManConsensus> solutionClass) {
		System.out.println("Testing consensus with crashes, long timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(1, 100), solutionClass, new CliqueDistNetwork.Factory(2), 100))
			return;
		System.out.println("Testing consensus with crashes, medium timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(1, 20), solutionClass, new CliqueDistNetwork.Factory(2), 100))
			return;
		System.out.println("Testing consensus with crashes, short timeout before crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(1, 2), solutionClass, new CliqueDistNetwork.Factory(2), 100))
			return;
		System.out.println("Testing consensus with crashes, immediate crash..");
		if (!ProblemTester.testProblem(new ConsensusProblemInstance(1, 0), solutionClass, new CliqueDistNetwork.Factory(2), 100))
			return;
		System.out.println("All good!");
	}
}
