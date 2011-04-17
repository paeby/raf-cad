package examples.broadcast;

import common.problem.Solution;

public interface Broadcast extends Solution {
	public void broadcast(Object message);
	
	public Object getBroadcastedObject();
}
