package examples.queue;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface FifoQueue extends Solution {
	void add(int value, ConcurrentSystem system, ProcessInfo callerInfo);
	int remove(ConcurrentSystem system, ProcessInfo callerInfo);
}
