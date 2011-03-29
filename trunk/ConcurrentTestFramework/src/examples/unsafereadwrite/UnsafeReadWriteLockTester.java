package examples.unsafereadwrite;

public class UnsafeReadWriteLockTester {
	
	public static void testUnsafeReadWriteLock(UnsafeReadWriteLock unsafeReadWriteLock) {
		int[][] readerSteps = new int[][] {
				{ 1, 1 },
				{ 1, 1, 1 },
				{ 10, 10 },
				{ 20, 20, 20, 20, 20, 20, 20, 20 } };
		int[][] writerSteps = new int[][] {
				{ 1, 1, 1 },
				{ 1 },
				{ 10, 10, 10, 10, 10 },
				{ 10, 10 } };
		int[] times = new int[] { 100, 100, 200, 200 };
		
		int maxNumberOfParallelReads = -1;
		for (int i = 0; i < readerSteps.length; i++) {
			for (int timeIndex = 0; timeIndex < times[i]; timeIndex++) {
				if (timeIndex % 20 == 0)
					System.out.print('.');
				
				Integer solution = new UnsafeReadWriteLockProblemInstance(readerSteps[i], writerSteps[i]).testOnce(unsafeReadWriteLock);
				if (solution == null) {
					System.out.println("\nNe ponaša se dobro");
					return;
				} else if (maxNumberOfParallelReads < solution)
					maxNumberOfParallelReads = solution;
			}
		}
		if (maxNumberOfParallelReads == 1) {
			System.out.println("\nNi jednom se nije dogodilo paralelno čitanje iz dveju niti");
		} else 
			System.out.println("\nAll good");
		
		// System.out.println("2 writers, 20 readers");
		// if (!ProblemTester.testProblem(new
		// ReadersWriterLockTimedProblemInstance(500, 20, 2), readersWriterLock,
		// 5))
		// return;
		// System.out.println("\n20 writers, 2 readers");
		// ProblemTester.testProblem(new
		// ReadersWriterLockTimedProblemInstance(500, 2, 20), readersWriterLock,
		// 5);
	}
	
}
