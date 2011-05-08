package kids.dist.examples.causalbroadcast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.RandomMessage;
import kids.dist.util.TimeoutCounter;

public class CausalBroadcastProblemInstance extends DefaultProblemInstance<CausalBroadcast> implements RandomizableProblemInstance<CausalBroadcast> {
	final int numberOfMessages;
	final List<Object> messages;
	
	public CausalBroadcastProblemInstance(int numberOfMessages) {
		super();
		this.numberOfMessages = numberOfMessages;
		this.messages = new ArrayList<Object>(numberOfMessages);
	}
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		messages.clear();
		for (int i = 0; i < numberOfMessages; i++)
			messages.add(new RandomMessage());
	}
	
	boolean areInOrder(List<Object> msgList) {
		if (msgList.isEmpty())
			return true;
		Iterator<Object> msgIterator = msgList.iterator();
		Object message = msgIterator.next();
		for (Object shouldBeMessage : messages) {
			if (!shouldBeMessage.equals(message))
				return false;
			if (!msgIterator.hasNext())
				return true;
			message = msgIterator.next();
		}
		return true;
	}
	
	@Override
	public SingleProcessTester<CausalBroadcast> createSingleProcessTester(DistributedManagedSystem system, CausalBroadcast mySolution, final int threadIndex) {
		return new SingleProcessTester<CausalBroadcast>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, CausalBroadcast solution) {
				if (threadIndex == 0) {
					for (Object msg : messages)
						solution.broadcast(msg);
				}
				TimeoutCounter counter = new TimeoutCounter(2000);
				while (!counter.timeRanOut()) {
					system.handleMessages();
					List<Object> receivedMessages = solution.getReceivedMessages();
					if (receivedMessages != null) {
						if (!areInOrder(receivedMessages)) {
							return TesterVerdict.FAIL;
						}
						if (receivedMessages.size() == numberOfMessages) {
							return TesterVerdict.SUCCESS;
						}
					}
				}
				return TesterVerdict.TIMEOUT;
			}
		};
		
	}
}
