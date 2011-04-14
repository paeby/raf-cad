package common.tasks;

import core.DistributedManagedSystem;

public interface Task {
	public void execute(DistributedManagedSystem system);
}
