package examples.snapshot;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Snapshot extends Solution {
	void updateValue(int index, byte value, int length, ConcurrentSystem system, ProcessInfo callerInfo);
	
	int[] getAllValues(int length, ConcurrentSystem system, ProcessInfo callerInfo);
	
}
