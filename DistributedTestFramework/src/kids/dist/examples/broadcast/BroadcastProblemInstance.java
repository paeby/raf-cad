package kids.dist.examples.broadcast;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.RandomMessage;
import kids.dist.util.TimeoutCounter;

public class BroadcastProblemInstance extends DefaultProblemInstance<Broadcast> implements RandomizableProblemInstance<Broadcast> {
	Object msg;
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		msg = new RandomMessage();
	}
	
	@Override
	public SingleProcessTester<Broadcast> createSingleProcessTester(DistributedManagedSystem system, Broadcast mySolution, final int threadIndex) {
		return new SingleProcessTester<Broadcast>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, Broadcast solution) {
				if (threadIndex == 0) {
					solution.broadcast(msg);
					Object result = solution.getBroadcastedMessage();
					if (result == null || !result.equals(msg))
						return TesterVerdict.FAIL;
					else
						return TesterVerdict.SUCCESS;
				} else {
					TimeoutCounter counter = new TimeoutCounter(500);
					while (!counter.timeRanOut()) {
						system.handleMessages();
						Object received = solution.getBroadcastedMessage();
						if (received != null)
							if (received.equals(msg))
								return TesterVerdict.SUCCESS;
							else
								return TesterVerdict.FAIL;
					}
					return TesterVerdict.TIMEOUT;
				}
			}
		};
		
	}
}
