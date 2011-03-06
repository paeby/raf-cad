package examples.casn;

import core.impl.problem.ProblemTester;

public class CompareAndSet2Tester {
	public static void testCas2(CompareAndSet2 cas2) {
		System.out.println("Validation test");
		int[] dummyTest = new int[] { 5 };
		if (!ProblemTester.testProblem(new CompareAndSet2ProblemInstance(dummyTest), cas2, 100))
			return;
		System.out.println("Dummy test");
		int[] twoThreadTest = new int[] { 5, 5 };
		if (!ProblemTester.testProblem(new CompareAndSet2ProblemInstance(twoThreadTest), cas2))
			return;
		System.out.println("Thorough test");
		int[] thoroughTest = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		if (!ProblemTester.testProblem(new CompareAndSet2ProblemInstance(thoroughTest), cas2))
			return;	
	}
}
