package kids.dist.examples.nodecounter;

import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;

public class NodeCounterProblemInstance extends DefaultProblemInstance<NodeCounter> {
	@Override
	public SingleProcessTester<NodeCounter> createSingleProcessTester(DistributedManagedSystem system, NodeCounter mySolution, int index) {
		return new SingleProcessTester<NodeCounter>() {
			@Override
			public TesterVerdict test(DistributedManagedSystem system, NodeCounter solution) {
				int processId = system.getProcessId();
				system.addLogLine("Requesting numberOfNodes for process #" + processId);
				int number = solution.getNumberOfNodes();
				if (number == system.getNumberOfNodes()) {
					system.addLogLine("SUCCESS! Process #" + processId + " returned the correct number of nodes: " + number);
					return TesterVerdict.SUCCESS;
				} else {
					system.addLogLine("FAILURE! Process #" + processId + " returned an incorrect number of nodes: " + number);
					return TesterVerdict.FAIL;
				}
			}
		};
	}
}
