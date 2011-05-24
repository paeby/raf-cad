package kids.dist.examples.crashdetect;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class CrashDetectProblemInstance extends DefaultProblemInstance<CrashDetect> implements RandomizableProblemInstance<CrashDetect> {
	volatile int crashedIndex;
	volatile int crashedId;
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		crashedIndex = (int) (Math.random() * system.getNumberOfNodes());
		crashedId = -1;
	}
	
	@Override
	public SingleProcessTester<CrashDetect> createSingleProcessTester(DistributedManagedSystem system, CrashDetect mySolution, final int threadIndex) {
		return new SingleProcessTester<CrashDetect>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, CrashDetect solution) {
				if (threadIndex == crashedIndex) {
					crashedId = system.getProcessId();
					system.setTimebombForThisThread(100);
				}
				
				while (crashedId == -1)
					system.yield();
				
				int result = solution.getCrashedId();
				if (crashedId != result)
					return TesterVerdict.FAIL;
				else
					return TesterVerdict.SUCCESS;
			}
		};
	}
}
