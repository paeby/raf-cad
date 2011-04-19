package kids.dist.common.tasks;

import kids.dist.core.DistributedManagedSystem;

public interface Task {
	public void execute(DistributedManagedSystem system);
}
