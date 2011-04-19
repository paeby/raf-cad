package kids.dist.examples.broadcast;

import kids.dist.common.problem.Solution;

public interface Broadcast extends Solution {
	public void broadcast(Object message);
	
	public Object getBroadcastedMessage();
}
