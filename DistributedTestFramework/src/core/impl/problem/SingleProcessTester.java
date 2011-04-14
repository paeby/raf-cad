package core.impl.problem;

import common.problem.Solution;

import core.DistributedManagedSystem;

public interface SingleProcessTester<T extends Solution> {
	
	public TesterVerdict test(DistributedManagedSystem system, T solution);
	
}
