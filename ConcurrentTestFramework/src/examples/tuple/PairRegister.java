package examples.tuple;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface PairRegister extends Solution {
	int[] read(ConcurrentSystem system, ProcessInfo callerInfo);
	void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo);
}
