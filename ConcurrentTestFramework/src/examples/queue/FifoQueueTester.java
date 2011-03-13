package examples.queue;

import core.impl.problem.ProblemTester;


public class FifoQueueTester {
	
	public static void testFifoQueue(FifoQueue fifoQueue) {
		System.out.println("Dummy test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(1, 10), fifoQueue, 100))
			return;
		System.out.println("Real test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(2, 30), fifoQueue, 100))
			return;
		System.out.println("Thorough test");
		if (!ProblemTester.testProblem(new FifoQueueProblemInstance(5, 100), fifoQueue, 30))
			return;
	}
	
}
