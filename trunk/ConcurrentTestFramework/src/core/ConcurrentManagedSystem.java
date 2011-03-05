package core;

import common.ConcurrentSystem;

import core.impl.InstructionType;

public interface ConcurrentManagedSystem extends ConcurrentSystem {

	void taskStarted();

	void taskFinished();

	void actionCalled();

	void addLogLine(String line);

	void startSimAndWaitToFinish();

	void incStat(InstructionType type);
	
//	Random getRandom();
}
