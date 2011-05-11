package kids.dist.core.impl.problem;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.common.problem.ProblemInstance;
import kids.dist.common.problem.Solution;
import kids.dist.common.tasks.Task;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.FrameworkDecidedToKillProcessException;

public abstract class DefaultProblemInstance<T extends Solution> implements ProblemInstance<T> {
	@Override
	public boolean execute(DistributedManagedSystem managedSystem, final Class<? extends T> solutionClass) {
		int numOfNodes = managedSystem.getNumberOfNodes();
		
		final AtomicBoolean allOk = new AtomicBoolean(true);
		final AtomicInteger countAlive = new AtomicInteger(numOfNodes);
		
		for (int i = 0; i < numOfNodes; i++) {
			final int threadIndex = i;
			final T mySolution = createInstance(managedSystem, solutionClass);
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(DistributedManagedSystem system) {
					RuntimeException ex = null;
					try {
						system.setMySolution(mySolution);
						if (mySolution instanceof InitiableSolution)
							((InitiableSolution) mySolution).initialize();
						SingleProcessTester<T> myTester = createSingleProcessTester(system, mySolution, threadIndex);
						system.handleMessages();
						if (myTester != null) {
							TesterVerdict verdict = myTester.test(system, mySolution);
							switch (verdict) {
							case FAIL:
								allOk.set(false);
								break;
							case TIMEOUT:
								system.addLogLine("Previše vremena je prošlo bez tačnog odgovora");
								allOk.set(false);
								break;
							case SUCCESS:
								break;
							}
						}
					} catch (FrameworkDecidedToKillProcessException exc) {
						return;
					} catch (RuntimeException thr) {
						ex = thr;
						return;
					} finally {
						if (ex != null)
							throw ex;
						
						countAlive.decrementAndGet();
						while (countAlive.get() > 0)
							system.handleMessages();
					}
				}
			});
		}
		
		managedSystem.startSimAndWaitToFinish();
		return allOk.get();
	}
	
	public SingleProcessTester<T> createSingleProcessTester(final DistributedManagedSystem system, final T mySolution, int threadIndex) {
		return null;
	}
	
	private T createInstance(DistributedManagedSystem system, Class<? extends T> solutionClass) {
		T mySolution;
		try {
			mySolution = solutionClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Solution not instantiated", e);
		}
		
		for (Field field : solutionClass.getDeclaredFields()) {
			if (DistributedSystem.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				try {
					field.set(mySolution, system);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				break;
			}
		}
		return mySolution;
	}
	
}
