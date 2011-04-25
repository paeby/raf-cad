package kids.dist.examples.pingeveryone;

import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class PingEveryoneProblemInstance extends DefaultProblemInstance<PingEveryone> implements RandomizableProblemInstance<PingEveryone> {
	
	AtomicInteger theOneWhoDies = new AtomicInteger();
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		theOneWhoDies.set(-1);
	}
	
	@Override
	public SingleProcessTester<PingEveryone> createSingleProcessTester(DistributedManagedSystem system, PingEveryone mySolution, int index) {
		return new SingleProcessTester<PingEveryone>() {
			boolean hasSentAPing = false;
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, PingEveryone solution) {
				if (theOneWhoDies.compareAndSet(-1, system.getProcessId())) {
					int ticks = 0;
					while (Math.random() < 0.9)
						ticks++;
					system.setTimebombForThisThread(ticks);
				}
				if (!hasSentAPing) {
					solution.pingNeighbourhood();
				}
				while (solution.hasBeenPinged() < 10 * (system.getProcessNeighbourhood().length-1)) {
					system.handleMessages();
				}
				return TesterVerdict.SUCCESS;
			}
		};
	}
	
}
