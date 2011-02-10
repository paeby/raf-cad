package common.tasks;

import common.ConcurrentSystem;

public interface Task {
	public void execute(ConcurrentSystem system);
}
