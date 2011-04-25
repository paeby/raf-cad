package kids.dist.examples.nodecounter;

import kids.dist.core.impl.problem.ProblemTester;

public class NodeCounterTester {
	
	public static void testNodeCounter(Class<? extends NodeCounter> solutionClass) {
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, 10, 100, 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, 10, 25, 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, 20, 25, 100);
		ProblemTester.testProblem(new NodeCounterProblemInstance(), solutionClass, 50, 25, 100);
		System.out.println("All ok!");
	}
	
}
