package kids.dist.common.problem;

import kids.dist.core.DistributedManagedSystem;

public interface ProblemInstance<T extends Solution> {
	boolean execute(DistributedManagedSystem managedSystem, Class<? extends T> solutionClass);
}
