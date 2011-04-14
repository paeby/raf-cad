package core.impl.problem;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import common.DistributedSystem;
import common.problem.ProblemInstance;
import common.problem.Solution;
import common.tasks.Task;

import core.DistributedManagedSystem;

public abstract class DefaultProblemInstance<T extends Solution> implements ProblemInstance<T> {
	@Override
	public boolean execute(DistributedManagedSystem managedSystem, final Class<? extends T> solutionClass) {
		int numOfNodes = managedSystem.getNumberOfNodes();
		
		final AtomicBoolean allOk = new AtomicBoolean(true);
		
		for (int i = 0; i < numOfNodes; i++) {
			final int threadIndex = i;
			final T mySolution = createInstance(managedSystem, solutionClass);
			managedSystem.startTaskConcurrently(new Task() {
				@Override
				public void execute(DistributedManagedSystem system) {
					SingleProcessTester<T> myTester = createSingleProcessTester(system, mySolution, threadIndex);
					system.handleMessages(mySolution);
					if (myTester != null) {
						TesterVerdict verdict = myTester.test(system, mySolution);
						switch (verdict) {
						case FAIL:
							allOk.set(false);
							return;
						case TIMEOUT:
							system.addLogLine("Previše vremena je prošlo bez tačnog odgovora");
							allOk.set(false);
							return;
						case SUCCESS:
							return;
						}
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
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Solution not instantiated", e);
		} catch (IllegalAccessException e) {
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
