package kids.dist.examples.pingeveryone;

import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class PingEveryoneProblemInstance extends DefaultProblemInstance<PingEveryone> {
	
	@Override
	public SingleProcessTester<PingEveryone> createSingleProcessTester(DistributedManagedSystem system, PingEveryone mySolution, int index) {
		return new SingleProcessTester<PingEveryone>() {
			boolean hasSentAPing = false;
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, PingEveryone solution) {
				if (!hasSentAPing) {
					solution.pingNeighbourhood();
				}
				while (solution.hasBeenPinged() < 10 * system.getProcessNeighbourhood().length) {
					system.handleMessages();
				}
				return TesterVerdict.SUCCESS;
			}
		};
	}
	
}
