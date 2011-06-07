package kids.dist.examples.tokenmutex;

import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class TokenMutexProblemInstance extends DefaultProblemInstance<TokenMutex> implements RandomizableProblemInstance<TokenMutex> {
	
	public TokenMutexProblemInstance() {}
	
	final AtomicInteger lockHoldCount = new AtomicInteger(0);
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		lockHoldCount.set(0);
	}
	
	@Override
	public SingleProcessTester<TokenMutex> createSingleProcessTester(DistributedManagedSystem system, TokenMutex mySolution, final int threadIndex) {
		return new SingleProcessTester<TokenMutex>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, TokenMutex solution) {
				if (threadIndex == 0)
					solution.createToken();
				int count = (int) (20 * Math.random());
				for (int i = 0; i < count; i++) {
					system.yield();
					
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
