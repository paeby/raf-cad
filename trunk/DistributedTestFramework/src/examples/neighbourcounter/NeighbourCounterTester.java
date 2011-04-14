package examples.neighbourcounter;

import core.impl.problem.ProblemTester;

public class NeighbourCounterTester {
	
	public static void testNeighbourCounter(Class<? extends NeighbourCounter> solutionClass) {
		ProblemTester.testProblem(new NeighbourCounterProblemInstance(), solutionClass, 100, 25, 100);
	}
	
}
