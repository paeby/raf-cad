package examples.snapshot;

import core.impl.problem.ProblemTester;

public class SnapshotTester {

	
//	int arrayLength, int readers, int readIterations, int writers, int writeIterations, int writeYields
	
	public static void testSnapshot(Snapshot snapshot) {
//		int[][] desc = new int[][] { 
//				{4, 1, 2, 2, 3, 3},
//				{8, 1, 10, 4, 10, 0},
//				{8, 1, 10, 4, 10, 2},
//				{8, 1, 10, 4, 10, 5},
//		}; 								
//		int[] times = new int[] { 50, 50, 50, 50 };
//		
//		for (int i = 0; i < desc.length; i++)
//			if (!ProblemTester.testProblem(new SnapshotFixedProblemInstance(desc[i][0], desc[i][1], desc[i][2], desc[i][3], desc[i][4], desc[i][5]), snapshot, times[i]))
//				return;
//		
		
		
		ProblemTester.testProblem(new SnapshotTimedProblemInstance(10, 500), snapshot, 10);
	}
	
}
