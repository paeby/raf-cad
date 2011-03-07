package examples.tuple;

import core.impl.problem.ProblemTester;

public class PairRegisterTester {
	public static void testPairRegister(PairRegister reg2) {
		System.out.println("Validation test");
		int[] dummyTest = new int[] { 10 };
		if (!ProblemTester.testProblem(new PairRegisterProblemInstance(dummyTest), reg2, 100))
			return;
		System.out.println("Dummy test");
		int[] twoThreadTest = new int[] { 10, 10};
		if (!ProblemTester.testProblem(new PairRegisterProblemInstance(twoThreadTest), reg2))
			return;
		System.out.println("Thorough test");
		int[] thoroughTest = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
		if (!ProblemTester.testProblem(new PairRegisterProblemInstance(thoroughTest), reg2))
			return;	
	}
}
