package kids.dist.core.impl.problem;

import kids.dist.common.problem.Solution;
import kids.dist.core.DistributedManagedSystem;

public interface SingleProcessTester<T extends Solution> {
	
	public TesterVerdict test(DistributedManagedSystem system, T solution);
	
}
