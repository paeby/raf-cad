package kids.dist.core.impl.message;

import java.util.LinkedList;

import kids.dist.core.MessageBundle;
import kids.dist.core.MessageQueue;

public class FifoMessageQueue implements MessageQueue {
	private final LinkedList<MessageBundle> messages = new LinkedList<MessageBundle>();
	
	@Override
	public void add(MessageBundle message) {
		messages.addLast(message);
	}
	
	@Override
	public boolean isEmpty() {
		return messages.isEmpty();
	}
	
	@Override
	public MessageBundle getMessage() {
		if (messages.isEmpty())
			return null;
		else
			return messages.removeFirst();
	}
	
	@Override
	public int size() {
		return messages.size();
	}
}
