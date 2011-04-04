package examples.copyonwriteset;

import seminarski1.IntSet;
import seminarski1.tester.IntSetProblemInstance;
import useful.ObjectHelper;

public class CopyOnWriteSetTester {
	public static final class IntSetWrapper implements IntSet {
		private final CopyOnWriteSet set;
		
		public IntSetWrapper(CopyOnWriteSet set) {
			super();
			this.set = set;
		}
		
		@Override
		public boolean addInt(int value) {
			return this.set.add(value);
		}
		
		@Override
		public boolean contains(int value) {
			return this.set.contains(value);
		}
		
		@Override
		public boolean removeInt(int value) {
			return this.set.remove(value);
		}
		
		@Override
		public String toString() {
			return this.set.toString();
		}
	}
	
	private static boolean testWithOnlyOneThread(CopyOnWriteSet set) {
		if (!test(set, false, false))
			return false;
		if (!test(set, false, true))
			return false;
		return true;
	}
	
	private static boolean testWithMultipleThreads(CopyOnWriteSet set) {
		if (!test(set, true, false))
			return false;
		if (!test(set, true, true))
			return false;
		return true;
	}
	
	public static void testCopyOnWriteSet(CopyOnWriteSet set) {
		if (!testWithOnlyOneThread(set))
			return;
		if (!testWithMultipleThreads(set))
			return;
		System.out.println("All good");
	}
	
	private static boolean test(final CopyOnWriteSet set, boolean useMultipleThreads, boolean allowOperationsThatFail) {
		try {
			int[] sampleSizes = new int[] { 5, 8, 10 };
			int[] stepsPerThread = new int[] { 4000, 8000, 12000 };
			int[] threads = new int[] { 4, 4, 4 };
			
			for (int i = 0; i < sampleSizes.length; i++) {
				try {
					for (int testNo = 0; testNo < 40; testNo++) {
						IntSetProblemInstance problemInstance = new IntSetProblemInstance(sampleSizes[i], useMultipleThreads ? threads[i] : 1, stepsPerThread[i], allowOperationsThatFail);
						String[] errors = problemInstance.testAndGetErrorMessages(new IntSetWrapper(ObjectHelper.copy(set)));
						if (errors == null)
							System.out.print('.');
						else {
							reportErrors(errors);
							return false;
						}
					}
				} finally {
					System.out.println();
				}
			}
		} finally {
			IntSetProblemInstance.shutdownExecutor();
		}
		return true;
	}
	
	private static void reportErrors(String[] errors) {
		System.out.println("Pronađene su sledeće greške:");
		for (int i = 0; i < errors.length; i++)
			System.out.println(" * " + errors[i]);
	}
}
