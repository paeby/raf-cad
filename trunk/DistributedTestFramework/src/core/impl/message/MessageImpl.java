package core.impl.message;

import core.Message;

public class MessageImpl implements Message {
	private final int from, to, type;
	private final Object msg;
	
	public MessageImpl(int from, int to, int type, Object msg) {
		super();
		this.from = from;
		this.to = to;
		this.type = type;
		this.msg = msg;
	}
	@Override
	public int getFrom() {
		return from;
	}
	
	@Override
	public int getTo() {
		return to;
	}
	
	@Override
	public int getType() {
		return type;
	}
	
	@Override
	public Object getMsg() {
		return msg;
	}
	
}
