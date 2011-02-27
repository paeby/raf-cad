package examples.snapshot;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.Register;

import core.impl.problem.ProblemTester;

public class SnapshotTester {
	private static final class SnapshotNaive implements Snapshot {
		@Override
		public int getArrayLength() {
			return 10;
		}
		
		@Override
		public int[] getAllValues(ConcurrentSystem system, ProcessInfo callerInfo) {
			int[] values = new int[getArrayLength()];
			for (int i = 0; i < values.length; i++)
				values[i] = system.getRegister(i).read();
			return values;
		}
		
		@Override
		public void updateValue(int index, int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			system.getRegister(index).write(value);
		}
	}
	
	private static final class SnapshotAtomic implements Snapshot {
		@Override
		public int getArrayLength() {
			return 10;
		}
		
		private void collect(int[][] values, ConcurrentSystem system, ProcessInfo callerInfo) {
			for (int i = 0; i < values.length; i++) {
				values[i][0] = system.getRegister(i).read();
				values[i][1] = system.getRegister(values.length + i).read();
			}
		}
		
		private boolean areMatricesEqual(int m1[][], int m2[][]) {
			if (m1.length != m2.length)
				return false;
			for (int i = 0; i < m1.length; i++) {
				if (m1[i].length != m2[i].length)
					return false;
				for (int j = 0; j < m1[i].length; j++)
					if (m1[i][j] != m2[i][j])
						return false;
			}
			return true;
		}
		
		@Override
		public int[] getAllValues(ConcurrentSystem system, ProcessInfo callerInfo) {
			int[][] temp1, temp2, helper;
			temp1 = new int[getArrayLength()][];
			temp2 = new int[temp1.length][];
			for (int i = 0; i < temp1.length; i++) {
				temp1[i] = new int[2];
				temp2[i] = new int[2];
			}
			
			collect(temp1, system, callerInfo);
			while (true) {
				collect(temp2, system, callerInfo);
				if (areMatricesEqual(temp1, temp2)) {
					int[] result = new int[temp1.length];
					for (int i = 0; i < result.length; i++)
						result[i] = temp1[i][0];
					return result;
				} else {
					helper = temp1;
					temp1 = temp2;
					temp2 = helper;
				}
			}
		}
		
		@Override
		public void updateValue(int index, int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register tsRegister = system.getRegister(-system.getPID() - 1);
			int ts = tsRegister.read();
			tsRegister.write(++ts);
			system.getRegister(index + getArrayLength()).write(ts);
			system.getRegister(index).write(value);
		}
	}
	
	private static final class SnapshotAtomicLockFree implements Snapshot {
		private static final int ARRAY_LENGTH = 10;
		
		@Override
		public int getArrayLength() {
			return ARRAY_LENGTH;
		}
		
		private void collect(int[][] values, ConcurrentSystem system, ProcessInfo callerInfo) {
			for (int i = 0; i < ARRAY_LENGTH; i++) {
				values[i][0] = system.getRegister(i).read();
				values[i][1] = system.getRegister(ARRAY_LENGTH + i).read();
				for (int j = 0; j < ARRAY_LENGTH; j++)
					values[i][j + 2] = system.getRegister(ARRAY_LENGTH * (2 + i) + j).read();
			}
		}
		
		private boolean areMatricesEqual(int m1[][], int m2[][]) {
			if (m1.length != m2.length)
				return false;
			for (int i = 0; i < m1.length; i++) {
				if (m1[i].length != m2[i].length)
					return false;
				for (int j = 0; j < m1[i].length; j++)
					if (m1[i][j] != m2[i][j])
						return false;
			}
			return true;
		}
		
		private void copyMatrix(int to[][], int from[][]) {
			for (int i = 0; i < from.length; i++)
				for (int j = 0; j < from[i].length; j++)
					to[i][j] = from[i][j];
		}
		
		@Override
		public int[] getAllValues(ConcurrentSystem system, ProcessInfo callerInfo) {
			int[][] temp1, temp2, temp3, helper;
			temp1 = new int[ARRAY_LENGTH][];
			temp2 = new int[ARRAY_LENGTH][];
			temp3 = new int[ARRAY_LENGTH][];
			for (int i = 0; i < ARRAY_LENGTH; i++) {
				temp1[i] = new int[(2 + ARRAY_LENGTH) * ARRAY_LENGTH];
				temp2[i] = new int[(2 + ARRAY_LENGTH) * ARRAY_LENGTH];
				temp3[i] = new int[(2 + ARRAY_LENGTH) * ARRAY_LENGTH];
			}
			
			collect(temp1, system, callerInfo);
			copyMatrix(temp2, temp1);
			while (true) {
				collect(temp3, system, callerInfo);
				if (areMatricesEqual(temp3, temp2)) {
					int[] result = new int[ARRAY_LENGTH];
					for (int i = 0; i < ARRAY_LENGTH; i++)
						result[i] = temp3[i][0];
					return result;
				}
				for (int i = 0; i < ARRAY_LENGTH; i++)
					if (temp3[i][1] >= temp1[i][1] + 2) {
						int[] result = new int[ARRAY_LENGTH];
						for (int j = 0; j < ARRAY_LENGTH; j++)
							result[j] = temp3[i][j + 2];
						return result;
					}
				helper = temp3;
				temp3 = temp2;
				temp2 = helper;
			}
		}
		
		@Override
		public void updateValue(int index, int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register tsRegister = system.getRegister(-system.getPID() - 1);
			int ts = tsRegister.read();
			tsRegister.write(++ts);
			
			int[] snapshot = getAllValues(system, callerInfo);
			for (int i = ARRAY_LENGTH - 1; i >= 0; i--)
				system.getRegister(ARRAY_LENGTH * (index + 2) + i).write(snapshot[i]);
			system.getRegister(index + ARRAY_LENGTH).write(ts);
			system.getRegister(index).write(value);
		}
	}
	
	public static void testSnapshot(Snapshot snapshot) {
		ProblemTester.testProblem(new SnapshotProblemInstance(500), snapshot, 10);
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		testSnapshot(new SnapshotNaive());
		System.out.println("Atomic but with starvation:");
		testSnapshot(new SnapshotAtomic());
		System.out.println("Atomic & lock free:");
		testSnapshot(new SnapshotAtomicLockFree());
	}
}
