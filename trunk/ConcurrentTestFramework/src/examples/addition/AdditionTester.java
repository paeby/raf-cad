package examples.addition;

import core.impl.problem.ProblemTester;

public class AdditionTester {
	public static void testAddition(Addition addition) {
		int[][] desc = new int[][] { 
				{2, 2},
				{2, 5}, 
				{5, 10},
				{10, 5}, 
				{30, 50}}; 								
		int[] times = new int[] { 100, 100, 100, 100, 20 };
		
		for (int i = 0; i < desc.length; i++)
			if (!ProblemTester.testProblem(new AdditionProblemInstance(desc[i][0], desc[i][1]), addition, times[i]))
				return;

	}
	
}
