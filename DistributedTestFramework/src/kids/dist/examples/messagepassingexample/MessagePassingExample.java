package kids.dist.examples.messagepassingexample;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.Solution;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class MessagePassingExample implements Solution {
	DistributedSystem system;
	
	public void testMessagePassing() {
		int myId = system.getProcessId(), otherId = system.getProcessNeighbourhood()[0];
		if (myId < otherId)
			for (int i = 0; i < 30; i++)
				system.sendMessage(otherId, 0, i);
	}
	
	@Override
	public void messageReceived(int from, int type, Object message) {
		System.out.println(message);
	}
	
	public static void main(String[] args) {
		ProblemTester.testProblem(new DefaultProblemInstance<MessagePassingExample>() {
			@Override
			public SingleProcessTester<MessagePassingExample> createSingleProcessTester(DistributedManagedSystem system, MessagePassingExample mySolution, int threadIndex) {
				return new SingleProcessTester<MessagePassingExample>() {
					@Override
					public TesterVerdict test(DistributedManagedSystem system, MessagePassingExample solution) {
						solution.testMessagePassing();
						system.handleMessages();
						return TesterVerdict.SUCCESS;
					}
				};
			}
		}, MessagePassingExample.class, 2, 100, 1);
	}
}
