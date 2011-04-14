package core;

public interface MessageQueue {
	public void add(Message message);
	
	public boolean isEmpty();
	
	public Message getMessage();
}
