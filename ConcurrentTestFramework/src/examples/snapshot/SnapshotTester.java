package examples.snapshot;

import core.impl.problem.ProblemTester;

public class SnapshotTester {

	
	public static void testSnapshot(Snapshot snapshot) {
		ProblemTester.testProblem(new SnapshotProblemInstance(500), snapshot, 10);
	}
	
}
