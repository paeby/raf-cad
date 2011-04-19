package kids.dist.core;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.Solution;
import kids.dist.common.tasks.Task;
import kids.dist.core.impl.InstructionType;

public interface DistributedManagedSystem extends DistributedSystem {
	void taskStarted();
	
	void taskFinished();
	
	void actionCalled();
	
	void addLogLine(String line);
	
	void startSimAndWaitToFinish();
	
	void startTaskConcurrently(Task task);
	
	void incStat(InstructionType type);
	
	int getNumberOfNodes();
	
	public void handleMessages();

	void setMySolution(Solution solution);
}
