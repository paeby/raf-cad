package examples.crossing;

import core.impl.problem.ProblemTester;

public class CrossingTester {


	
	public static void testCrossing(Crossing crossing) {
		int[][][] trafficDesc = new int[][][] { 
				{{2,1,1}, {1,1,1}, {1,1,1}},
				{{3, 10, 1}, {3, 10, 2}},
				{{3, 10, 1}, {3, 10, 0}, {3, 10, 4}, {3, 10, 1}, {2, 20, 1}}}; 								
		int[] times = new int[] { 100, 200, 200 };
		
		for (int i = 0; i < trafficDesc.length; i++)
			if (!ProblemTester.testProblem(new CrossingProblemInstance(trafficDesc[i]), crossing, times[i]))
				return;
		
	}
	
}
