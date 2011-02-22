package examples.snapshot;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Snapshot extends Solution {
	void updateValue(int index, int value, ConcurrentSystem system, ProcessInfo callerInfo);
	
	int[] getAllValues(ConcurrentSystem system, ProcessInfo callerInfo);
	
	int getArrayLength();
}
