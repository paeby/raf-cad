package common;

import common.problem.Solution;

public interface DistributedSystem {
	public int getProcessId();
	
	public int[] getProcessNeighbourhood();
	
	void handleMessages(Solution solution);
	
	public void sendMessage(int destinationId, int type, Object message);
}
