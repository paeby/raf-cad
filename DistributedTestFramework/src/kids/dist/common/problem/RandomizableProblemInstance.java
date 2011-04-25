package kids.dist.common.problem;

import kids.dist.core.DistributedManagedSystem;

public interface RandomizableProblemInstance<T extends Solution> extends ProblemInstance<T> {
	void randomize(DistributedManagedSystem system);
}
