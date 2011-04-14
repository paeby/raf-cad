package core;

public interface Message {
	
	public int getFrom();
	
	public int getTo();
	
	public int getType();
	
	public Object getMsg();
	
}
