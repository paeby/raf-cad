package examples.mutex;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Mutex extends Solution {
	public void lock(ConcurrentSystem system, ProcessInfo info);
	
	public void unlock(ConcurrentSystem system, ProcessInfo info);
}
