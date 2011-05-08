package kids.dist.examples.mutex;

import kids.dist.common.problem.Solution;

public interface Mutex extends Solution {
	public void lock();
	
	public void unlock();
}
