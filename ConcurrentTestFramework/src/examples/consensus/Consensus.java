package examples.consensus;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Consensus extends Solution {
	/**
	 * Each call proposes a positive value, 
	 * and calls in all threads must return same value, 
	 * and that value must be one proposed by some process.
	 *  
	 * @param value
	 * @param system
	 * @param callerInfo
	 * @return
	 */
	int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo);

}
