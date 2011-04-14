package core.impl.message;

import java.util.LinkedList;
import java.util.Random;

import core.Message;
import core.MessageQueue;

public class RandomMessageQueue implements MessageQueue {
	private final LinkedList<Message> messages = new LinkedList<Message>();
	private final Random random = new Random(); // threadlocal jer postoji jedan
												// queue po threadu
	
	@Override
	public void add(Message message) {
		messages.add(random.nextInt(messages.size() + 1), message);
	}
	
	@Override
	public boolean isEmpty() {
		return messages.isEmpty();
	}
	
	@Override
	public Message getMessage() {
		if (messages.isEmpty())
			return null;
		else
			return messages.removeFirst();
	}
	
}
