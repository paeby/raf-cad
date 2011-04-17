package examples.pingeveryone;

import common.problem.Solution;

public interface PingEveryone extends Solution {
	public void pingNeighbourhood();
	
	public int hasBeenPinged();
}
