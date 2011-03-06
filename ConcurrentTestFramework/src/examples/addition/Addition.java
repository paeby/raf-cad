package examples.addition;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Addition extends Solution {
	int addAndGet(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo);
}
