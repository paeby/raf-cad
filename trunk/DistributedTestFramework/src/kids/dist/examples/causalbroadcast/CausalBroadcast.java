package kids.dist.examples.causalbroadcast;

import java.util.List;

import kids.dist.common.problem.Solution;

public interface CausalBroadcast extends Solution {
	public void broadcast(Object msg);
	
	public List<Object> getReceivedMessages();
}
