package examples.mutex;

import core.impl.problem.ProblemTester;

public class MutexTester {
	
	public static void testMutex(Mutex mutex) {
		ProblemTester.testProblem(new MutexProblemInstance(), mutex, 100);
	}
	
}
