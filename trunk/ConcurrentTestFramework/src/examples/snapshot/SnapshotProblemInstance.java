package examples.snapshot;

import common.problem.ProblemInstance;

import core.ConcurrentManagedSystem;

public class SnapshotProblemInstance implements ProblemInstance<Snapshot> {

	@Override
	public boolean execute(ConcurrentManagedSystem system, Snapshot snapshot) {
		return false;
	}
	
}
