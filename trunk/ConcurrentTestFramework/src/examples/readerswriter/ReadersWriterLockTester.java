package examples.readerswriter;

import core.impl.problem.ProblemTester;

public class ReadersWriterLockTester {
	

	
	public static void testReadersWriterLock(ReadersWriterLock readersWriterLock) {
		int[][] readerSteps = new int[][] { {1,1}, {10,10}, {20, 20, 20, 20, 20, 20, 20, 20}};
		int[][] writerSteps = new int[][] { {1,1,1}, {10,10,10,10,10}, {10,10}};
		int[] times = new int[] { 100, 200, 200 };
		
		for (int i = 0; i < readerSteps.length; i++)
			if (!ProblemTester.testProblem(new ReadersWriterLockFixedProblemInstance(readerSteps[i], writerSteps[i]), readersWriterLock, times[i]))
				return;
		
		
		
//		System.out.println("2 writers, 20 readers");
//		if (!ProblemTester.testProblem(new ReadersWriterLockTimedProblemInstance(500, 20, 2), readersWriterLock, 5))
//			return;
//		System.out.println("\n20 writers, 2 readers");
//		ProblemTester.testProblem(new ReadersWriterLockTimedProblemInstance(500, 2, 20), readersWriterLock, 5);
	}
	

}
