package examples.counter;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Counter extends Solution {
	void inc(ConcurrentSystem system, ProcessInfo callerInfo);
	
	int getValue(ConcurrentSystem system, ProcessInfo callerInfo);
}
