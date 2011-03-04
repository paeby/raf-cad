package examples.crossing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.problem.ProblemInstance;
import common.tasks.Task;

import core.ConcurrentManagedSystem;

public class CrossingProblemInstance implements ProblemInstance<Crossing> {
	private final int[][] trafficDesc;
	
	public CrossingProblemInstance(int[][] trafficDesc) {
		this.trafficDesc = trafficDesc;
	}
	
	@Override
	public boolean execute(final ConcurrentManagedSystem managedSystem, final Crossing solution) {
		
		final AtomicBoolean correct = new AtomicBoolean(true);
		
		final AtomicIntegerArray curTraffic = new AtomicIntegerArray(trafficDesc.length);
		
		int total = 0;
		for(int i = 0;i<trafficDesc.length;i++) 			
			total += trafficDesc[i][0];
		
		int counter = 0;
		for(int i = 0;i<trafficDesc.length;i++) {			
			final int curDirection = i;
			final int curSteps = trafficDesc[i][1];
			final int yieldSteps = trafficDesc[i][2];
			for(int j = 0;j<trafficDesc[i][0];j++) {
				final ProcessInfo callerInfo = new ProcessInfo(counter, total);
				counter ++;
				managedSystem.startTaskConcurrently(new Task() {
					
					@Override
					public void execute(ConcurrentSystem system) {
						for (int i = 0;i<curSteps;i++) {
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquiring lock");
							solution.enterCrossing(curDirection, trafficDesc.length, system, callerInfo);
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader acquired lock");

							curTraffic.incrementAndGet(curDirection);
							for (int d = 0;d < trafficDesc.length;d++)
								if (d != curDirection)
									if (curTraffic.get(d)>0)
										incorrect();
							
							for (int y = 0;y < yieldSteps;y++) {
								system.yield();
								for (int d = 0;d < trafficDesc.length;d++)
									if (d != curDirection)
										if (curTraffic.get(d)>0)
											incorrect();
							}
							
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader realising lock");
							solution.leaveCrossing(curDirection, trafficDesc.length, system, callerInfo);
							managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " reader realised lock");

							curTraffic.decrementAndGet(curDirection);
						}
					}
					
					@Override
					public String toString() {
						return "Reader";
					}
					
					private void incorrect() {
						managedSystem.addLogLine("\t\t\tcid=" + callerInfo.getCurrentId() + " incorrect state: " + curTraffic.toString());
						correct.set(false);
					}
				});
			}
		}
		

		managedSystem.startSimAndWaitToFinish();
		return correct.get();
	
	}

}
