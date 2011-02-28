package examples.counter;

import core.impl.problem.ProblemTester;

public class CounterTester {
	
	
	public static void testCounter(Counter counter) {
		int[][] counts = new int[][] { { 1, 1 }, { 2, 1, 1 }, { 3, 2, 3, 1 } };
		int[] times = new int[] { 100, 200, 1000 };
		
		for (int i = 0; i < counts.length; i++)
			if (!ProblemTester.testProblem(new CounterProblemInstance(counts[i]), counter, times[i]))
				return;
	}
	

}
