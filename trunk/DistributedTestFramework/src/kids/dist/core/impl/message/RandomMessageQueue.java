package kids.dist.core.impl.message;

import java.util.LinkedList;
import java.util.Random;

import kids.dist.core.MessageBundle;
import kids.dist.core.MessageQueue;

public class RandomMessageQueue implements MessageQueue {
	private final LinkedList<MessageBundle> messages = new LinkedList<MessageBundle>();
	private final Random random = new Random(); // threadlocal jer postoji jedan
												// queue po threadu
	
	@Override
	public void add(MessageBundle message) {
		messages.add(random.nextInt(messages.size() + 1), message);
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
	
}
