package examples.causalbroadcast;

import java.util.List;

import common.problem.Solution;

public interface CausalBroadcast extends Solution {
	public void broadcast(Object msg);
	
	public List<Object> getReceivedMessages();
}
