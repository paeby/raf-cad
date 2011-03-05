package examples.snapshot;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class SnapshotFixedProblemInstance implements ProblemInstance<Snapshot> {
	
	private final int arrayLength;
	private final int readers;
	private final int readIterations;
	private final int writers;
	private final int writeIterations;
	private final int writeYields;

	public SnapshotFixedProblemInstance(int arrayLength, int readers, int readIterations, int writers, int writeIterations, int writeYields) {
		this.arrayLength = arrayLength;
		this.readers = readers;
		this.readIterations = readIterations;
		this.writers = writers;
		this.writeIterations = writeIterations;
		this.writeYields = writeYields;
	}



	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Snapshot snapshot) {
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final ArrayList<int[]> changes = new ArrayList<int[]>(); 
		
		// atomicity test
		for (int i = 0;i<writers;i++) {
			final int curId = i;
			final ProcessInfo callerInfo = new ProcessInfo(curId, readers+writers);;
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) { 
					int positionOfNextWrite = 0;
					boolean writeOnes = true;
					for (int it = 0;it<writeIterations;it++) {
						if (writeOnes) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling updateValue(" + positionOfNextWrite + ", 1)");
							snapshot.updateValue(positionOfNextWrite++, (byte)1, arrayLength, system, callerInfo);

							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " updateValue finished");
							if (positionOfNextWrite == arrayLength) {
								positionOfNextWrite = arrayLength - 1;
								writeOnes = false;
							}
						} else {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling updateValue(" + positionOfNextWrite + ", 0)");
							snapshot.updateValue(positionOfNextWrite--, (byte)0, arrayLength, system, callerInfo);
							
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " updateValue finished");
							if (positionOfNextWrite < 0) {
								positionOfNextWrite = 0;
								writeOnes = true;
							}
						}
						
						for (int i = 0; i < writeYields ; i++)
							managedSystem.yield();
					}
				}
			});
		}
		

		for (int i = 0;i<readers;i++) {
			final ProcessInfo callerInfo = new ProcessInfo(writers + i, readers+ writers);
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) {
					for (int it = 0;it<readIterations;it++) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling getAllValues");
						int[] values = snapshot.getAllValues(arrayLength, managedSystem, callerInfo);
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " getAllValues finished");
						boolean foundZero = false;
						for (int i = 0; i < values.length; i++)
							if (values[i] == 0)
								foundZero = true;
							else if (foundZero) {
								managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " **** invalid getAllValues result!!!! **** ");
								correct.set(false);
								break;
							}
					}
				}
			});
		}
		
		managedSystem.startSimAndWaitToFinish();
		return correct.get();
	}
}
