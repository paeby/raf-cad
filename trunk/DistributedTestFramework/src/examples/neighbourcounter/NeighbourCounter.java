package examples.neighbourcounter;

import common.problem.Solution;

public interface NeighbourCounter extends Solution {
	public void pingNeighbours();
	
	public int getNumberOfMessagesReceived();
}
