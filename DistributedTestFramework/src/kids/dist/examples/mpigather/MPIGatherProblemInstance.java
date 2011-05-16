package kids.dist.examples.mpigather;

import java.util.concurrent.atomic.AtomicReferenceArray;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.RandomMessage;

public class MPIGatherProblemInstance extends DefaultProblemInstance<MPIGather> implements RandomizableProblemInstance<MPIGather> {
	AtomicReferenceArray<Object> msgs = null;
	volatile int bcaster;
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		if (msgs == null)
			msgs = new AtomicReferenceArray<Object>(system.getNumberOfNodes());
		for (int i = 0; i < system.getNumberOfNodes(); i++)
			msgs.set(i, new RandomMessage());
		bcaster = (int) (Math.random() * system.getNumberOfNodes());
	}
	
	@Override
	public SingleProcessTester<MPIGather> createSingleProcessTester(DistributedManagedSystem system, MPIGather mySolution, final int threadIndex) {
		return new SingleProcessTester<MPIGather>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, MPIGather solution) {
				if (threadIndex == bcaster) {
					solution.offer(threadIndex, msgs.get(threadIndex));
					Object[] result = solution.gather();
					if (result == null || result.length != msgs.length())
						return TesterVerdict.FAIL;
					for (int i = 0; i < result.length; i++)
						if (result[i] != msgs.get(i))
							return TesterVerdict.FAIL;
					return TesterVerdict.SUCCESS;
				} else {
					solution.offer(threadIndex, msgs.get(threadIndex));
					return TesterVerdict.SUCCESS;
				}
			}
		};
	}
}
