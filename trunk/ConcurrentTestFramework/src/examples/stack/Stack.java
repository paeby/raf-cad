package examples.stack;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Stack extends Solution {
	public void push(int n, ConcurrentSystem system, ProcessInfo callerInfo);
	
	public int poll(ConcurrentSystem system, ProcessInfo callerInfo);
}
