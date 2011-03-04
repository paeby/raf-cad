package common;

import java.util.HashMap;
import java.util.Map;

public class ProcessInfo {
	final int currentId;
	final int totalProcesses;
	
	final Map<Integer, Integer> threadLocals;
	
	public ProcessInfo(int currentId, int totalProcesses) {
		this.currentId = currentId;
		this.totalProcesses = totalProcesses;
		this.threadLocals = new HashMap<Integer, Integer>();
	}
	
	public int getCurrentId() {
		return currentId;
	}
	
	public int getTotalProcesses() {
		return totalProcesses;
	}
	
	public Map<Integer, Integer> getThreadLocals() {
		return threadLocals;
	}
	
	public int getThreadLocal(int key) {
		Integer value = threadLocals.get(key);
		return value==null?0:value.intValue();
	}

	public void putThreadLocal(int key, int value) {
		threadLocals.put(key, value);
	}
	
}
