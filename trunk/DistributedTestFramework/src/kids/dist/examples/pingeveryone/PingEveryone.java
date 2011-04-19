package kids.dist.examples.pingeveryone;

import kids.dist.common.problem.Solution;

public interface PingEveryone extends Solution {
	public void pingNeighbourhood();
	
	public int hasBeenPinged();
}
