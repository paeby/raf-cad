package kids.dist.examples.neighbourcounter;

import kids.dist.common.problem.Solution;

public interface NeighbourCounter extends Solution {
	public void pingNeighbours();
	
	public int getNumberOfMessagesReceived();
}
