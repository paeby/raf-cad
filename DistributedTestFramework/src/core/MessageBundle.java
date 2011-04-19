package core;

public interface MessageBundle {
	
	public int getFrom();
	
	public int getTo();
	
	public int getType();
	
	public Object getMsg();
}
