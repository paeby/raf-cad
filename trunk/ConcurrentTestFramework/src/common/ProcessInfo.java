package common;

import java.util.Map;
import java.util.TreeMap;

import core.impl.IntArrayComparator;

public class ProcessInfo {
	final int currentId;
	final int totalProcesses;
	
	final Map<int[], Integer> threadLocals;
	
	public ProcessInfo(int currentId, int totalProcesses) {
		this.currentId = currentId;
		this.totalProcesses = totalProcesses;
		this.threadLocals = new TreeMap<int[], Integer>(new IntArrayComparator());
	}
	
	public int getCurrentId() {
		return currentId;
	}
	
	public int getTotalProcesses() {
		return totalProcesses;
	}
	
	public Map<int[], Integer> getThreadLocals() {
		return threadLocals;
	}
	
	public int getThreadLocal(int... key) {
		Integer value = threadLocals.get(key);
		return value==null?0:value.intValue();
	}

	public void putThreadLocal(int key, int value) {
		threadLocals.put(new int[] {key}, value);
	}

	public void putThreadLocal(int[] key, int value) {
		threadLocals.put(key, value);
	}
	
}
