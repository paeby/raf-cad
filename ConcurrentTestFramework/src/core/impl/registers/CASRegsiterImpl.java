package core.impl.registers;

import java.util.Arrays;

import common.registers.CASRegister;

import core.ConcurrentManagedSystem;
import core.impl.InstructionType;

public class CASRegsiterImpl implements CASRegister {
	final ConcurrentManagedSystem system;
	final int[] registerId;
	int value;
	
	public CASRegsiterImpl(ConcurrentManagedSystem system, int[] registerId, int value) {
		this.system = system;
		this.registerId = registerId;
		this.value = value;
	}
	
	@Override
	public int read() {
		system.actionCalled();
		system.incStat(InstructionType.READ);
		system.addLogLine("reg" + Arrays.toString(registerId) + ".read() = " + value);
		return value;
	}
	
	@Override
	public void write(int value) {
		system.actionCalled();
		system.incStat(InstructionType.WRITE);
		system.addLogLine("reg" + Arrays.toString(registerId) + ".write(" + value + ")");
		this.value = value;
	}
	
	@Override
	public boolean compareAndSet(int expect, int update) {
		system.actionCalled();
		String details = "reg" + Arrays.toString(registerId) + "(" + value + ").cas(" + expect + ", " + update + ") = ";
		system.incStat(InstructionType.CAS);
		if (value == expect) {
			system.addLogLine(details + "true");
			value = update;
			return true;
		} else {
			system.addLogLine(details + "false");
			return false;
		}
	}
	
	@Override
	public String toString() {
		return "reg" + Arrays.toString(registerId) + "=" + Integer.toString(value);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(registerId);
		result = prime * result + value;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CASRegsiterImpl other = (CASRegsiterImpl) obj;
		if (!Arrays.equals(registerId, other.registerId))
			return false;
		if (value != other.value)
			return false;
		return true;
	}
	
}
