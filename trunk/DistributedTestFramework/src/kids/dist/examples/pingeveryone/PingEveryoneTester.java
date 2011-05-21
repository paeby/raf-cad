package kids.dist.examples.pingeveryone;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.SparseDistNetwork;

public class PingEveryoneTester {
	
	public static void testPingEveryone(Class<? extends PingEveryone> solutionClass) {
		ProblemTester.testProblem(new PingEveryoneProblemInstance(), solutionClass, new SparseDistNetwork.Factory(100, 25), 20);
	}
	
}
