package examples.mutex;

import core.impl.problem.ProblemTester;

public class MutexTester {
	
	public static void testMutex(Mutex mutex) {
		int[][] desc = new int[][] { 
				{2,5}, 
				{5, 10},
				{10, 5}}; 								
		int[] times = new int[] { 100, 100, 100 };
		
		for (int i = 0; i < desc.length; i++)
			if (!ProblemTester.testProblem(new MutexProblemInstance(desc[i][0], desc[i][1]), mutex, times[i]))
				return;

	}
	
}
