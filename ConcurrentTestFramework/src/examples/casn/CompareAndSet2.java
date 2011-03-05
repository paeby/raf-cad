package examples.casn;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface CompareAndSet2 extends Solution {
	int[] read(ConcurrentSystem system, ProcessInfo callerInfo);
	void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo);
	boolean compareAndSet(int expected1, int expected2, int update1, int update2, ConcurrentSystem system, ProcessInfo callerInfo);
}
