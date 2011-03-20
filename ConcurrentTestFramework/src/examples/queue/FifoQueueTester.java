package examples.queue;

import core.impl.problem.ProblemTester;


public class FifoQueueTester {
	
	public static void testFifoQueue(FifoQueue fifoQueue) {
		System.out.println("Dummy test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(1, 10, true), fifoQueue, 100))
			return;
		System.out.println("Shallow correctness test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(2, 3, false), fifoQueue, 200))
			return;
		System.out.println("Add all then remove all test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(2, 20, true), fifoQueue, 100))
			return;
		System.out.println("Simultaneous reading and writing");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(1, 5, false), fifoQueue, 200))
			return;
		System.out.println("Thorough test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(5, 100, false), fifoQueue, 30))
			return;
	}
	
}
