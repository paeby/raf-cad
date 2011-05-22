package kids.dist.examples.crashdetect;

import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.network.CliqueDistNetwork;

public class CrashDetectTester {
	public static void testCrashDetect(Class<? extends CrashDetect> crashDetectClass) {
		if (!ProblemTester.testProblem(new CrashDetectProblemInstance(), crashDetectClass, new CliqueDistNetwork.Factory(20), 100))
			return;
		System.out.println("All good!");
	}
}
