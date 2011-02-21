package common;

import common.registers.CASRegister;
import common.registers.Register;
import common.tasks.Task;

public interface ConcurrentSystem {
	Register getRegister(int index);
	CASRegister getCASRegister(int index);
	void startTaskConcurrently(Task task);
	int getPID();

	void transactionStarted();
	void transactionEnded();
}
