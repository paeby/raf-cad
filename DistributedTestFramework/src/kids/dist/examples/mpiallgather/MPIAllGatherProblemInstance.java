package kids.dist.examples.mpiallgather;

import java.util.concurrent.atomic.AtomicReferenceArray;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.RandomMessage;

public class MPIAllGatherProblemInstance extends DefaultProblemInstance<MPIAllGather> implements RandomizableProblemInstance<MPIAllGather> {
	AtomicReferenceArray<Object> msgs = null;

	@Override
	public void randomize(DistributedManagedSystem system) {
		if (msgs == null)
			msgs = new AtomicReferenceArray<Object>(system.getNumberOfNodes());
		for (int i = 0; i < system.getNumberOfNodes(); i++)
			msgs.set(i, new RandomMessage());
	}

	@Override
	public SingleProcessTester<MPIAllGather> createSingleProcessTester(DistributedManagedSystem system, MPIAllGather mySolution, final int threadIndex) {
		return new SingleProcessTester<MPIAllGather>() {

			@Override
			public TesterVerdict test(DistributedManagedSystem system, MPIAllGather solution) {
				Object[] result = solution.gather(threadIndex, msgs.get(threadIndex));
				if (result == null || result.length != msgs.length())
					return TesterVerdict.FAIL;
				for (int i = 0; i < result.length; i++)
					if (result[i] != msgs.get(i))
						return TesterVerdict.FAIL;
				return TesterVerdict.SUCCESS;
			}
		};
	}
}
