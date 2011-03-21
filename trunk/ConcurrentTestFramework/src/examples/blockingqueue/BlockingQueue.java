package examples.blockingqueue;

public interface BlockingQueue {
	public int remove();
	
	public void put(int value);
}
