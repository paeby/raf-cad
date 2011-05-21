package kids.dist.examples.nodecounter;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.core.network.SparseDistNetwork;

public class NodeCounterTester {
	
	public static void testNodeCounter(Class<? extends NodeCounter> solutionClass) {
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(10), 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, new SparseDistNetwork.Factory(10, 25), 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, new SparseDistNetwork.Factory(20, 25), 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, new SparseDistNetwork.Factory(50, 25), 100);
		System.out.println("All ok!");
	}
	
}
