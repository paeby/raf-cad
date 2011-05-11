package kids.dist.examples.mutex;

import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class MutexProblemInstance extends DefaultProblemInstance<Mutex> implements RandomizableProblemInstance<Mutex> {
	
	public MutexProblemInstance() {}
	
	final AtomicInteger lockHoldCount = new AtomicInteger(0);
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		lockHoldCount.set(0);
	}
	
	@Override
	public SingleProcessTester<Mutex> createSingleProcessTester(DistributedManagedSystem system, Mutex mySolution, final int threadIndex) {
		return new SingleProcessTester<Mutex>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, Mutex solution) {
				int count = (int)(20*Math.random());
				for (int i = 0; i < count; i++) {
					solution.lock();
					if (lockHoldCount.incrementAndGet() != 1) {
						system.addLogLine("Multiple processes inside the critical section detected");
						return TesterVerdict.FAIL;
					}
					
					system.yield();
					
					if (lockHoldCount.getAndDecrement() != 1) {
						system.addLogLine("Multiple processes inside the critical section detected");
						return TesterVerdict.FAIL;
					}
					solution.unlock();
				}
				return TesterVerdict.SUCCESS;
			}
		};
	}
}
