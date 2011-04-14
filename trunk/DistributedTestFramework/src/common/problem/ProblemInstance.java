package common.problem;

import core.DistributedManagedSystem;

public interface ProblemInstance<T extends Solution> {
	boolean execute(DistributedManagedSystem managedSystem, Class<? extends T> solutionClass);
}
