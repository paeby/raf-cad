package examples.stack;

import core.impl.problem.ProblemTester;

public class StackTester {
	
	public static void testStack(Stack lifo) {
		System.out.println("Dummy test");
		if (!ProblemTester.testProblem(new StackProblemInstance(1, 10, true), lifo, 100))
			return;
		System.out.println("Shallow correctness test");
		if (!ProblemTester.testProblem(new StackProblemInstance(2, 3, false), lifo, 200))
			return;
		System.out.println("Add all then remove all test");
		if (!ProblemTester.testProblem(new StackProblemInstance(2, 20, true), lifo, 100))
			return;
		System.out.println("Simultaneous reading and writing");
		if (!ProblemTester.testProblem(new StackProblemInstance(1, 5, false), lifo, 200))
			return;
		System.out.println("Thorough test");
		if (!ProblemTester.testProblem(new StackProblemInstance(5, 100, false), lifo, 30))
			return;
		System.out.println("\nAll good!");
	}
	
}
