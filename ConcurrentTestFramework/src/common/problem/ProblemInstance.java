package common.problem;

import core.ConcurrentManagedSystem;

public interface ProblemInstance<T extends Solution> {
	boolean execute(ConcurrentManagedSystem system, T solution);
}
