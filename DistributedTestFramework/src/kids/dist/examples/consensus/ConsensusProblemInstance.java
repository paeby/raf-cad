package kids.dist.examples.consensus;

import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class ConsensusProblemInstance extends DefaultProblemInstance<Consensus> implements RandomizableProblemInstance<Consensus> {
	final int numberOfDeadOnes;
	
	public ConsensusProblemInstance(int numberOfDeadones) {
		this.numberOfDeadOnes = numberOfDeadones;
	}
	
	AtomicInteger stigaoDo = new AtomicInteger(0);
	AtomicInteger accepted = new AtomicInteger(-1);
	AtomicInteger deadNodeCount = new AtomicInteger();
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		accepted.set(-1);
		deadNodeCount.set(numberOfDeadOnes);
	}
	
	@Override
	public SingleProcessTester<Consensus> createSingleProcessTester(DistributedManagedSystem system, Consensus mySolution, final int threadIndex) {
		return new SingleProcessTester<Consensus>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, Consensus solution) {
				if (deadNodeCount.get() > 0) {
					deadNodeCount.decrementAndGet();
					int ticks = 0;
					while (Math.random() < 0.97)
						ticks++;
					system.setTimebombForThisThread(ticks);
				}
				system.handleMessages();
				int myProposal = stigaoDo.incrementAndGet();
				system.addLogLine("Process #" + system.getProcessId() + " has been proposed a value " + myProposal);
				int result = solution.propose(myProposal);
				system.addLogLine("Process #" + system.getProcessId() + " has been agreed on a value " + result);
				if (accepted.compareAndSet(-1, result)) {
					system.addLogLine("SUCCESS! Process #" + system.getProcessId() + " has agreed on an value: " + result);
					return TesterVerdict.SUCCESS;
				} else {
					if (Math.abs(result - myProposal) > system.getNumberOfNodes()) {
						system.addLogLine("FAILURE! Process #" + system.getProcessId() + " has agreed on an impossible value: " + result);
						return TesterVerdict.FAIL;
					}
					if (result != accepted.get()) {
						system.addLogLine("FAILURE! Process #" + system.getProcessId() + " has agreed on " + result + ", different from the previously agreed value " + accepted.get());
						return TesterVerdict.FAIL;
					}
					system.addLogLine("SUCCESS! Process #" + system.getProcessId() + " has agreed on an value: " + result);
					return TesterVerdict.SUCCESS;
				}
			}
		};
		
	}
}
