package useful;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;

public class AtomicCounter {

	final int registerIndex;
	
	public AtomicCounter(int registerIndex) {
		this.registerIndex = registerIndex;
	}

	public int getAndAdd(int toAdd, ConcurrentSystem system, ProcessInfo callerInfo) {
		CASRegister reg = system.getCASRegister(registerIndex);
		while (true) {
			int value = reg.read();
			if (reg.compareAndSet(value, value+toAdd))
				return value;
		}
	}
	
	public int get(ConcurrentSystem system, ProcessInfo callerInfo) {
		CASRegister reg = system.getCASRegister(registerIndex);
		return reg.read();
	}
	
	public void set(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
		CASRegister reg = system.getCASRegister(registerIndex);
		reg.write(value);
	}
	
	public boolean compareAndSet(int expect, int update, ConcurrentSystem system, ProcessInfo callerInfo) {
		CASRegister reg = system.getCASRegister(registerIndex);
		return reg.compareAndSet(expect, update);
	}
}
