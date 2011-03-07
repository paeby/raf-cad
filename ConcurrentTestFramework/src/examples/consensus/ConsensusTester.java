package examples.consensus;

import core.impl.problem.ProblemTester;

public class ConsensusTester {
	public static void testConsensus(Consensus consensus) {
		int[] desc = new int[] {2, 3, 10, 100}; 								
		int[] times = new int[] { 100, 100, 100, 100 };
		
		for (int i = 0; i < desc.length; i++)
			if (!ProblemTester.testProblem(new ConsensusProblemInstance(desc[i]), consensus, times[i]))
				return;

	}
}
