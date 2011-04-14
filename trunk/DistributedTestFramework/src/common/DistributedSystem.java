package common;

public interface DistributedSystem {
	public int getProcessId();
	
	public int[] getProcessNeighbourhood();
	
	public void sendMessage(int destinationId, int type, Object message);
}
