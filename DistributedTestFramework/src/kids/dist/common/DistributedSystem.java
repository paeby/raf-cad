package kids.dist.common;

public interface DistributedSystem {
	public int getProcessId();
	
	public int[] getProcessNeighbourhood();
	
	void yield();
	
	public void sendMessage(int destinationId, int type, Object message);
}
