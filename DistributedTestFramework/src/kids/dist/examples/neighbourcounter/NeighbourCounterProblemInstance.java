package kids.dist.examples.neighbourcounter;

import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.TimeoutCounter;

public class NeighbourCounterProblemInstance extends DefaultProblemInstance<NeighbourCounter> {
	@Override
	public SingleProcessTester<NeighbourCounter> createSingleProcessTester(DistributedManagedSystem system, NeighbourCounter mySolution, int index) {
		return new SingleProcessTester<NeighbourCounter>() {
			@Override
			public TesterVerdict test(DistributedManagedSystem system, NeighbourCounter solution) {
				solution.pingNeighbours();
				TimeoutCounter tc = new TimeoutCounter(500);
				while (solution.getNumberOfMessagesReceived() < system.getProcessNeighbourhood().length) {
					system.handleMessages();
					if (tc.timeRanOut())
						return TesterVerdict.TIMEOUT;
				}
				system.yield();
				system.handleMessages();
				if (solution.getNumberOfMessagesReceived() > system.getProcessNeighbourhood().length)
					return TesterVerdict.FAIL;
				return TesterVerdict.SUCCESS;
			}
		};
	}
}
