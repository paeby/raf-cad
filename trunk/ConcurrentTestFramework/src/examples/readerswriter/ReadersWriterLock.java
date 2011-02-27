package examples.readerswriter;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface ReadersWriterLock extends Solution {
	public void lockRead(ConcurrentSystem system, ProcessInfo info);
	
	public void lockWrite(ConcurrentSystem system, ProcessInfo info);
	
	public void unlockRead(ConcurrentSystem system, ProcessInfo info);
	
	public void unlockWrite(ConcurrentSystem system, ProcessInfo info);
}
