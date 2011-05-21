package kids.dist.seminarski2;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kids.dist.common.problem.RandomizableProblemInstance;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.FrameworkDecidedToKillProcessException;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.util.RandomMessage;

public class DistributedHashTableProblemInstance extends DefaultProblemInstance<DistributedHashTable> implements RandomizableProblemInstance<DistributedHashTable> {
	
	final DHTElement[] dhtElements;
	final boolean allowOverwrites, allowCrashes;
	volatile int dyingIndex;
	
	public DistributedHashTableProblemInstance(boolean allowOverwrites, boolean allowCrashes) {
		super();
		this.allowOverwrites = allowOverwrites;
		this.allowCrashes = allowCrashes;
		
		this.dhtElements = new DHTElement[256];
		for (int i = 0; i < 256; i++)
			this.dhtElements[i] = new DHTElement(i);
	}
	
	@Override
	public void randomize(DistributedManagedSystem system) {
		for (DHTElement element : dhtElements) {
			element.clear();
		}
		if (allowCrashes)
			dyingIndex = (int) (Math.random() * system.getNumberOfNodes());
	}
	
	@Override
	public SingleProcessTester<DistributedHashTable> createSingleProcessTester(DistributedManagedSystem system, DistributedHashTable mySolution, final int threadIndex) {
		return new SingleProcessTester<DistributedHashTable>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, DistributedHashTable solution) {
				for (int i = 0; i < 20; i++) {
					TesterVerdict testGetResult = testGet(system, solution);
					if (testGetResult != TesterVerdict.SUCCESS)
						return testGetResult;
					
					system.yield();
					
					TesterVerdict testPutResult = testPut(system, solution);
					if (testPutResult != TesterVerdict.SUCCESS)
						return testPutResult;
					
					system.yield();
					if (allowCrashes && dyingIndex == threadIndex) {
						if (Math.random() < 0.3d) {
							throw new FrameworkDecidedToKillProcessException();
						}
					}
				}
				return TesterVerdict.SUCCESS;
			}
			
			TesterVerdict testGet(DistributedManagedSystem system, DistributedHashTable solution) {
				DHTElement element = null;
				try {
					do {
						element = dhtElements[(int) (Math.random() * 256)];
					} while (!element.workingIndices.isEmpty());
					element.workingIndices.add(threadIndex);
					
					Object result = solution.get(element.id);
					if (result == null) {
						if (!element.possibleObjects.isEmpty()) {
							system.addLogLine("ERROR: Object returned by get is null, where a non-null object should be inside: id = " + element.id);
							return TesterVerdict.FAIL;
						}
					} else {
						boolean found = false;
						for (Object possibility : element.possibleObjects)
							if (possibility == result) {
								found = true;
								break;
							}
						if (!found) {
							system.addLogLine("ERROR: Object returned by get should not be here: id = " + element.id);
							return TesterVerdict.FAIL;
						}
					}
					return TesterVerdict.SUCCESS;
				} finally {
					if (element != null) {
						if (!element.workingIndices.remove(threadIndex) || !element.workingIndices.isEmpty())
							throw new IllegalStateException();

						if (allowOverwrites || element.waitingIndices.size() <= 1) {
							element.workingIndices.addAll(element.waitingIndices);
							element.waitingIndices.clear();
						} else {
							Iterator<Integer> iterator = element.waitingIndices.iterator();
							int next = iterator.next();
							element.workingIndices.add(next);
							iterator.remove();
						}
						element.firstWorkingIsInside = false;
					}
				}
			}
			
			TesterVerdict testPut(DistributedManagedSystem system, DistributedHashTable solution) {
				DHTElement element = null;
				try {
					element = dhtElements[(int) (Math.random() * 256)];
					
					if (element.workingIndices.isEmpty())
						element.workingIndices.add(threadIndex);
					else
						element.waitingIndices.add(threadIndex);
					while (!element.workingIndices.contains(threadIndex))
						system.yield();
					
					if (allowOverwrites && !element.firstWorkingIsInside) {
						element.firstWorkingIsInside = true;
						element.possibleObjects.clear();
					}
					
					if (allowOverwrites || element.possibleObjects.isEmpty()) {
						Object newObject = new RandomMessage();
						element.possibleObjects.add(newObject);
						solution.put(element.id, newObject);
					}
					
					system.yield();
					
					Object result = solution.get(element.id);
					if (result == null) {
						system.addLogLine("ERROR: Added object under id " + element.id + " hasn't been found");
						return TesterVerdict.FAIL;
					}
					{
						boolean found = false;
						for (Object possibility : element.possibleObjects)
							if (possibility == result) {
								found = true;
								break;
							}
						if (!found) {
							system.addLogLine("ERROR: Object returned by get should not be here: id = " + element.id);
							return TesterVerdict.FAIL;
						}
					}
					return TesterVerdict.SUCCESS;
				} finally {
					if (element != null) {
						element.workingIndices.remove(threadIndex);
						if (element.workingIndices.isEmpty()) {
							if (allowOverwrites || element.waitingIndices.size() <= 1) {
								element.workingIndices.addAll(element.waitingIndices);
								element.waitingIndices.clear();
							} else {
								Iterator<Integer> iterator = element.waitingIndices.iterator();
								int next = iterator.next();
								element.workingIndices.add(next);
								iterator.remove();
							}
							element.firstWorkingIsInside = false;
						}
					}
				}
			}
		};
	}
	
	class DHTElement {
		final int id;
		final Set<Object> possibleObjects;
		final Set<Integer> waitingIndices, workingIndices;
		volatile boolean firstWorkingIsInside = false;
		
		public DHTElement(int id) {
			super();
			this.id = id;
			this.possibleObjects = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
			this.waitingIndices = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
			this.workingIndices = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
		}
		
		void clear() {
			possibleObjects.clear();
			waitingIndices.clear();
			workingIndices.clear();
			firstWorkingIsInside = false;
		}
	}
}
