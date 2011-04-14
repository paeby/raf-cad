package core.impl;

import common.tasks.Task;

import core.DistributedManagedSystem;

public class WrappedRunnable implements Runnable {
	
	private final Task task;
	private final DistributedManagedSystem system;
	
	public WrappedRunnable(Task task, DistributedManagedSystem system) {
		super();
		this.task = task;
		this.system = system;
	}
	
	@Override
	public void run() {
		system.taskStarted();
		system.addLogLine("started task=" + task);
		
		task.execute(system);
		
		system.taskFinished();
	}
	
}
