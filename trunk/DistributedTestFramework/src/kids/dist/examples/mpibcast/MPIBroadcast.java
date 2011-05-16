package kids.dist.examples.mpibcast;

import kids.dist.examples.broadcast.Broadcast;

public interface MPIBroadcast extends Broadcast {
	public void broadcast(Object message);
	
	public Object getBroadcastedMessage();
}
