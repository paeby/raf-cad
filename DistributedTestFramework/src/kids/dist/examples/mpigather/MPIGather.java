package kids.dist.examples.mpigather;

import kids.dist.common.problem.Solution;

public interface MPIGather extends Solution {
	public void offer(int index, Object object);
	
	public Object[] gather();
}
