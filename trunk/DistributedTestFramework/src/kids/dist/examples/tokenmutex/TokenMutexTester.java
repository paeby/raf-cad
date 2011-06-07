package kids.dist.examples.tokenmutex;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;
import kids.dist.core.network.SparseDistNetwork;

public class TokenMutexTester {
	public static void testTokenMutex(Class<? extends TokenMutex> solutionClass) {
		System.out.println("Testing mutex in a clique..");
		if (!ProblemTester.testProblem(new TokenMutexProblemInstance(), solutionClass, new CliqueDistNetwork.Factory(20), 20))
			return;
		System.out.println("Testing mutex outside a clique..");
		if (!ProblemTester.testProblem(new TokenMutexProblemInstance(), solutionClass, new SparseDistNetwork.Factory(20, 25), 20))
			return;
		System.out.println("All good!");
	}
}
