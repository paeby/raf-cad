package core;

import common.DistributedSystem;
import common.tasks.Task;

import core.impl.InstructionType;

public interface DistributedManagedSystem extends DistributedSystem {
	void taskStarted();
	
	void taskFinished();
	
	void actionCalled();
	
	void addLogLine(String line);
	
	void startSimAndWaitToFinish();
	
	void startTaskConcurrently(Task task);
	
	void incStat(InstructionType type);
	
	int getNumberOfNodes();
	
	public void yield();
}
