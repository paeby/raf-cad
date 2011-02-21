package common;

public class ProcessInfo {
	final int currentId;
	final int totalProcesses;
	
	public ProcessInfo(int currentId, int totalProcesses) {
		this.currentId = currentId;
		this.totalProcesses = totalProcesses;
	}
	
	public int getCurrentId() {
		return currentId;
	}
	
	public int getTotalProcesses() {
		return totalProcesses;
	}
	
	
}
