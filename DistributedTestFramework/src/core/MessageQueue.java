package core;

public interface MessageQueue {
	public void add(MessageBundle message);
	
	public boolean isEmpty();
	
	public MessageBundle getMessage();
}
