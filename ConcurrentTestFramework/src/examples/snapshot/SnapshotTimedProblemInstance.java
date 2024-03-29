package examples.snapshot;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class SnapshotTimedProblemInstance implements ProblemInstance<Snapshot> {
	
	private final int arrayLength;

	private final long testLengthInMiliseconds;
	
	public SnapshotTimedProblemInstance(int arrayLength, long testLengthInMiliseconds) {
		this.arrayLength = arrayLength;
		this.testLengthInMiliseconds = testLengthInMiliseconds;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Snapshot snapshot) {
		if (arrayLength < 2) {
			System.out.println("Array length cannot be less than 2");
			return false;
		}
		
		final AtomicBoolean correct = new AtomicBoolean(true);
		final AtomicLong maximalTimeToRead = new AtomicLong(0);
		final AtomicLong maximalTimeToWrite = new AtomicLong(0);
		
		final Random rand = new Random(991);
		
		// atomicity test
		managedSystem.startTaskConcurrently(new Task() {
			@Override
			public void execute(ConcurrentSystem system) {
				ProcessInfo callerInfo = new ProcessInfo(0, 5);
				int positionOfNextWrite = 0;
				boolean writeOnes = true;
				long startingTime = System.currentTimeMillis();
				long timeBeforeWrite, now, ttw;
				while (correct.get() && System.currentTimeMillis() < startingTime + testLengthInMiliseconds) {
					if (writeOnes) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling updateValue(" + positionOfNextWrite + ", 1)");
						timeBeforeWrite = System.currentTimeMillis();
						snapshot.updateValue(positionOfNextWrite++, (byte)1, arrayLength, system, callerInfo);
						now = System.currentTimeMillis();
						ttw = maximalTimeToWrite.get();
						while (now - timeBeforeWrite > ttw) {
							if (!maximalTimeToWrite.compareAndSet(ttw, now - timeBeforeWrite))
								ttw = maximalTimeToWrite.get();
						}
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " updateValue finished");
						if (positionOfNextWrite == arrayLength) {
							positionOfNextWrite = arrayLength - 1;
							writeOnes = false;
						}
					} else {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling updateValue(" + positionOfNextWrite + ", 0)");
						timeBeforeWrite = System.currentTimeMillis();
						snapshot.updateValue(positionOfNextWrite--, (byte)0, arrayLength, system, callerInfo);
						now = System.currentTimeMillis();
						ttw = maximalTimeToWrite.get();
						while (now - timeBeforeWrite > ttw) {
							if (!maximalTimeToWrite.compareAndSet(ttw, now - timeBeforeWrite))
								ttw = maximalTimeToWrite.get();
						}
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " updateValue finished");
						if (positionOfNextWrite < 0) {
							positionOfNextWrite = 0;
							writeOnes = true;
						}
					}
					
					for (int i = rand.nextInt(3 * arrayLength / 2); i > 0; i--)
						managedSystem.yield();
				}
			}
		});
		
		for (int pid = 1; pid <= 5; pid++) {
			final ProcessInfo callerInfo = new ProcessInfo(pid, 5);
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(ConcurrentSystem system) {
					long startingTime = System.currentTimeMillis();
					long timeBeforeRead, now, ttr;
					while (correct.get() && System.currentTimeMillis() < startingTime + testLengthInMiliseconds) {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " calling getAllValues");
						timeBeforeRead = System.currentTimeMillis();
						int[] values = snapshot.getAllValues(arrayLength, managedSystem, callerInfo);
						now = System.currentTimeMillis();
						ttr = maximalTimeToRead.get();
						while (now - timeBeforeRead > ttr) {
							if (!maximalTimeToRead.compareAndSet(ttr, now - timeBeforeRead))
								ttr = maximalTimeToRead.get();
						}
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
		System.out.println("Maximal time to write: " + maximalTimeToWrite.get() + " ms");
		System.out.println("Maximal time to read: " + maximalTimeToRead.get() + " ms");
		return correct.get();
	}
	
	@Override
	public String toString() {
		return "SnapshotProblemInstance";
	}
}
