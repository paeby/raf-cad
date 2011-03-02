package examples.crossing;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.Solution;

public interface Crossing extends Solution {
	void enterCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo);
	
	void leaveCrossing(int curDirection, int totalDirections, ConcurrentSystem system, ProcessInfo callerInfo);

}
