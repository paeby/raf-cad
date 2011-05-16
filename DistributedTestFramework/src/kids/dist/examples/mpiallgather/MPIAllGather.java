package kids.dist.examples.mpiallgather;

import kids.dist.common.problem.Solution;

public interface MPIAllGather extends Solution {
	public Object[] gather(int index, Object object);
}
