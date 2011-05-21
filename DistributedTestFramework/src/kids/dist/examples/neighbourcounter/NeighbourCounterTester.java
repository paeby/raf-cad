package kids.dist.examples.neighbourcounter;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.SparseDistNetwork;

public class NeighbourCounterTester {
	
	public static void testNeighbourCounter(Class<? extends NeighbourCounter> solutionClass) {
		ProblemTester.testProblem(new NeighbourCounterProblemInstance(), solutionClass, new SparseDistNetwork.Factory(20, 25), 100);
	}
}
