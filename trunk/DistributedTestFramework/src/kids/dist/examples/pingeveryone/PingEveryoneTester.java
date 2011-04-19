package kids.dist.examples.pingeveryone;

import kids.dist.core.impl.problem.ProblemTester;

public class PingEveryoneTester {
	
	public static void testPingEveryone(Class<? extends PingEveryone> solutionClass) {
		ProblemTester.testProblem(new PingEveryoneProblemInstance(), solutionClass, 100, 25, 20);
	}
	
}
