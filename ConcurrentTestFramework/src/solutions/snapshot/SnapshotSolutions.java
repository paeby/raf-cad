package solutions.snapshot;

import java.util.Arrays;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.Register;

import examples.snapshot.Snapshot;
import examples.snapshot.SnapshotTester;

public class SnapshotSolutions {
	private static final class SnapshotNaive implements Snapshot {
		
		@Override
		public int[] getAllValues(int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			int[] values = new int[arrayLength];
			for (int i = 0; i < values.length; i++)
				values[i] = system.getRegister(i).read();
			return values;
		}
		
		@Override
		public void updateValue(int index, byte value, int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			system.getRegister(index).write(value);
		}
	}
	
	private static final class SnapshotNoTimestamps implements Snapshot {
		
		private int[] collect(int length, ConcurrentSystem system) {
			int[] res = new int[length];
			for(int i = 0;i<length;i++) {
				Register reg = system.getRegister(i);
				res[i] = reg.read();
			}
			return res;
		}
		
		
		@Override
		public int[] getAllValues(int length, ConcurrentSystem system, ProcessInfo callerInfo) {
			int[] previous = collect(length, system);
			
			while (true) {
				int[] cur = collect(length, system);
				if (Arrays.equals(previous, cur))
					return cur;
				previous = cur;
			}
		}
		
		@Override
		public void updateValue(int index, byte value, int length, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg = system.getRegister(index);
			reg.write((value & 255));
		}
	}
	
	private static final class SnapshotCorrect implements Snapshot {
	
		private int[] collect(int length, ConcurrentSystem system) {
			int[] res = new int[length];
			for(int i = 0;i<length;i++) {
				Register reg = system.getRegister(i);
				res[i] = reg.read();
			}
			return res;
		}
		
		
		@Override
		public int[] getAllValues(int length, ConcurrentSystem system, ProcessInfo callerInfo) {
			int[] previous = collect(length, system);
			
			while (true) {
				int[] cur = collect(length, system);
				if (Arrays.equals(previous, cur)) {
					int[] sol = new int[length];
					for(int i = 0;i<length;i++)
						sol[i] = cur[i] & 255;
					return sol;
				}
				previous = cur;					
			}
		}
		
		@Override
		public void updateValue(int index, byte value, int length, ConcurrentSystem system, ProcessInfo callerInfo) {
			int ts = callerInfo.getThreadLocal(0);
			callerInfo.putThreadLocal(0, ts+1);
			
			Register reg = system.getRegister(index);
			reg.write((ts<<8)| (value & 255));
		}
	}
	
	private static final class SnapshotAtomic implements Snapshot {
		
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
		public int[] getAllValues(int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			int[][] temp1, temp2, helper;
			temp1 = new int[arrayLength][];
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
		public void updateValue(int index, byte value, int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register tsRegister = system.getRegister(-system.getPID() - 1);
			int ts = tsRegister.read();
			tsRegister.write(++ts);
			system.getRegister(index + arrayLength).write(ts);
			system.getRegister(index).write(value);
		}
	}
	
	private static final class SnapshotAtomicLockFree implements Snapshot {
		
		private void collect(int[][] values, int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			for (int i = 0; i < arrayLength; i++) {
				values[i][0] = system.getRegister(i).read();
				values[i][1] = system.getRegister(arrayLength + i).read();
				for (int j = 0; j < arrayLength; j++)
					values[i][j + 2] = system.getRegister(arrayLength * (2 + i) + j).read();
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
		public int[] getAllValues(int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			int[][] temp1, temp2, temp3, helper;
			temp1 = new int[arrayLength][];
			temp2 = new int[arrayLength][];
			temp3 = new int[arrayLength][];
			for (int i = 0; i < arrayLength; i++) {
				temp1[i] = new int[(2 + arrayLength) * arrayLength];
				temp2[i] = new int[(2 + arrayLength) * arrayLength];
				temp3[i] = new int[(2 + arrayLength) * arrayLength];
			}
			
			collect(temp1, arrayLength, system, callerInfo);
			copyMatrix(temp2, temp1);
			while (true) {
				collect(temp3, arrayLength, system, callerInfo);
				if (areMatricesEqual(temp3, temp2)) {
					int[] result = new int[arrayLength];
					for (int i = 0; i < arrayLength; i++)
						result[i] = temp3[i][0];
					return result;
				}
				for (int i = 0; i < arrayLength; i++)
					if (temp3[i][1] >= temp1[i][1] + 2) {
						int[] result = new int[arrayLength];
						for (int j = 0; j < arrayLength; j++)
							result[j] = temp3[i][j + 2];
						return result;
					}
				helper = temp3;
				temp3 = temp2;
				temp2 = helper;
			}
		}
		
		@Override
		public void updateValue(int index, byte value, int arrayLength, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register tsRegister = system.getRegister(-system.getPID() - 1);
			int ts = tsRegister.read();
			tsRegister.write(++ts);
			
			int[] snapshot = getAllValues(arrayLength, system, callerInfo);
			for (int i = arrayLength - 1; i >= 0; i--)
				system.getRegister(arrayLength * (index + 2) + i).write(snapshot[i]);
			system.getRegister(index + arrayLength).write(ts);
			system.getRegister(index).write(value);
		}
	}
	

	public static void main(String[] args) {
		System.out.println("Naive:");
		SnapshotTester.testSnapshot(new SnapshotNaive());
		System.out.println("Without timestamps:");
		SnapshotTester.testSnapshot(new SnapshotNoTimestamps());
		System.out.println("Simple correct:");
		SnapshotTester.testSnapshot(new SnapshotCorrect());
		System.out.println("Atomic but with starvation:");
		SnapshotTester.testSnapshot(new SnapshotAtomic());
		System.out.println("Atomic & lock free:");
		SnapshotTester.testSnapshot(new SnapshotAtomicLockFree());
	}
}
