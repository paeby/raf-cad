package seminarski1.tester;

import seminarski1.IntSet;

public class IntSetTester {
	public static boolean testWithOnlyOneThread(IntSet intSet) {
		System.out.println(" ** Počinjem test sa jednom niti!");
		System.out.println("Prvo pozivam metode samo kada znam da će uspeti...");
		if (!test(intSet, false, false))
			return false;
		System.out.println("Sada pozivam metode kada ne moraju uspeti...");
		if (!test(intSet, false, true))
			return false;
		return true;
	}
	
	public static boolean testWithMultipleThreads(IntSet intSet) {
		System.out.println(" ** Počinjem test sa više niti!");
		System.out.println("Prvo pozivam metode samo kada znam da će uspeti...");
		if (!test(intSet, true, false))
			return false;
		System.out.println("Sada pozivam metode kada ne moraju uspeti...");
		if (!test(intSet, true, true))
			return false;
		return true;
	}
	
	public static boolean testEverything(IntSet intSet) {
		if (testWithOnlyOneThread(intSet))
			return testWithMultipleThreads(intSet);
		else
			return false;
	}
	
	public static boolean test(IntSet intSet, boolean useMultipleThreads, boolean allowOperationsThatFail) {
		try {
			int[] sampleSizes = new int[] { 5, 10, 10, 20 };
			int[] stepsPerThread = new int[] { 4000, 15000, 40000, 80000 };
			int[] threads = new int[] { 2, 4, 4, 8 };
			
			for (int i = 0; i < sampleSizes.length; i++) {
				try {
					for (int testNo = 0; testNo < 40; testNo++) {
						IntSetProblemInstance problemInstance = new IntSetProblemInstance(sampleSizes[i], useMultipleThreads ? threads[i] : 1, stepsPerThread[i], allowOperationsThatFail);
						String[] errors = problemInstance.testAndGetErrorMessages(intSet);
						if (errors == null)
							System.out.print('.');
						else {
							reportErrors(errors);
							return false;
						}
					}
				} finally {
					System.out.println();
				}
			}
		} finally {
			IntSetProblemInstance.shutdownExecutor();
		}
		System.out.println("Bravo!");
		return true;
	}
	
	private static void reportErrors(String[] errors) {
		System.out.println("Pronađene su sledeće greške:");
		for (int i = 0; i < errors.length; i++)
			System.out.println(" * " + errors[i]);
	}
}
