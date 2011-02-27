package core;

import java.util.Random;

import common.ConcurrentSystem;

public interface ConcurrentManagedSystem extends ConcurrentSystem {

	void taskStarted();

	void taskFinished();

	void actionCalled();

	void addLogLine(String line);

	void waitToFinish();
	
	Random getRandom();
}
