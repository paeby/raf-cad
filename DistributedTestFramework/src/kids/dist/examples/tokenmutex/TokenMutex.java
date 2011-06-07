package kids.dist.examples.tokenmutex;

import kids.dist.common.problem.Solution;

public interface TokenMutex extends Solution {
	public void createToken();
	
	public void lock();
	
	public void unlock();
}
