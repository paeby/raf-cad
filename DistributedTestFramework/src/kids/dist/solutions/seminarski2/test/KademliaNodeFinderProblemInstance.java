package kids.dist.solutions.seminarski2.test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.FrameworkDecidedToKillProcessException;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class KademliaNodeFinderProblemInstance extends DefaultProblemInstance<KademliaNodeFinder> implements RandomizableProblemInstance<KademliaNodeFinder> {
	
	final Random random = new Random();
	final AtomicIntegerArray array;
	final int myId, lookingFor;
	int processToCrash;
	
	public KademliaNodeFinderProblemInstance(boolean crashAProcess) {
		this(crashAProcess, -1, -1);
	}
	
	public KademliaNodeFinderProblemInstance(boolean crashAProcess, int myId, int lookingFor) {
		this.myId = myId;
		this.lookingFor = lookingFor;
		if (lookingFor == -1)
			array = new AtomicIntegerArray(20);
		else {
			array = new AtomicIntegerArray(1);
			array.set(0, lookingFor);
		}
		if (crashAProcess)
			processToCrash = 0;
		else
			processToCrash = -1;
	}
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		if (lookingFor == -1)
			for (int i = 0; i < array.length(); i++)
				array.set(i, random.nextInt(256));
		if (processToCrash >= 0)
			processToCrash = random.nextInt(system.getNumberOfNodes());
	}
	
	@Override
	public SingleProcessTester<KademliaNodeFinder> createSingleProcessTester(DistributedManagedSystem system, KademliaNodeFinder mySolution, final int threadIndex) {
		return new SingleProcessTester<KademliaNodeFinder>() {
			@Override
			public TesterVerdict test(DistributedManagedSystem system, KademliaNodeFinder solution) {
				int myId = system.getProcessId();
				if (lookingFor != -1 && myId != KademliaNodeFinderProblemInstance.this.myId)
					return TesterVerdict.SUCCESS;
				for (int i = 0; i < array.length(); i++) {
					int lookingFor = array.get(i);
					int result = solution.findNodeClosestTo(lookingFor);
					int distanceToResult = result ^ lookingFor;
					int distanceToMe = myId ^ lookingFor;
					if (distanceToResult > distanceToMe) {
						System.out.println(" NEUSPEH !!!!");
						System.out.println(" myId " + myId + ", searchingFor " + lookingFor);
						System.out.println(" returned: " + result);
						return TesterVerdict.FAIL;
					}
					system.handleMessages();
					if (processToCrash == threadIndex && random.nextDouble() < 0.2d) {
						throw new FrameworkDecidedToKillProcessException();
					}
				}
				return TesterVerdict.SUCCESS;
			}
		};
	}
}
