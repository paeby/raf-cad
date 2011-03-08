package solutions.tuple;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.tuple.PairRegister;
import examples.tuple.PairRegisterTester;

public class PairRegisterSolutions {
	final static class PairRegisterNaive implements PairRegister {
		@Override
		public int[] read(ConcurrentSystem system, ProcessInfo callerInfo) {
			int[] values = new int[2];
			values[0] = system.getRegister(0).read();
			values[1] = system.getRegister(1).read();
			return values;
		}
		
		@Override
		public void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo) {
			system.getRegister(0).write(value1);
			system.getRegister(1).write(value2);
		}
	}
	
	final static class PairRegisterTransaction implements PairRegister {
		
		@Override
		public int[] read(ConcurrentSystem system, ProcessInfo callerInfo) {
			system.transactionStarted();
			int v1 = system.getRegister(0).read();
			int v2 = system.getRegister(1).read();
			system.transactionEnded();
			return new int[] { v1, v2 };
		}
		
		@Override
		public void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo) {
			system.transactionStarted();
			system.getRegister(0).write(value1);
			system.getRegister(1).write(value2);
			system.transactionEnded();
		}
	}
	
	final static class PairRegisterCorrect implements PairRegister {
		
		@Override
		public int[] read(ConcurrentSystem system, ProcessInfo callerInfo) {
			int index = system.getCASRegister(-1).read();
			int[] value = new int[2];
			value[0] = system.getRegister(index * 2).read();
			value[1] = system.getRegister(index * 2 + 1).read();
			return value;
		}
		
		@Override
		public void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister indexRegister = system.getCASRegister(-1);
			CASRegister freeSpaceIndexRegister = system.getCASRegister(-2);
			int freeSpaceIndex;
			do {
				freeSpaceIndex = freeSpaceIndexRegister.read();
			} while (!freeSpaceIndexRegister.compareAndSet(freeSpaceIndex, freeSpaceIndex + 1));
			
			system.getRegister(2 * freeSpaceIndex + 2).write(value1);
			system.getRegister(2 * freeSpaceIndex + 3).write(value2);
			
			indexRegister.write(freeSpaceIndex + 1);
//			int index;
//			do {
//				index = indexRegister.read();
//			} while (!indexRegister.compareAndSet(index, freeSpaceIndex + 1));
		}		
	}
	
	final static class PairRegisterWaitFree implements PairRegister {
		
		@Override
		public int[] read(ConcurrentSystem system, ProcessInfo callerInfo) {
			int index = system.getRegister(-1).read();
			int[] value = new int[2];
			value[0] = system.getRegister(index).read();
			value[1] = system.getRegister(index + 1).read();
			return value;
		}
		
		@Override
		public void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register indexRegister = system.getCASRegister(-1);
			int freeSpaceIndex = callerInfo.getThreadLocal(0);
			freeSpaceIndex++;
			callerInfo.putThreadLocal(0, freeSpaceIndex + 1);
			
			int index = 2*callerInfo.getCurrentId() + (2*callerInfo.getTotalProcesses()) * freeSpaceIndex;
			system.getRegister(index).write(value1);
			system.getRegister(index+1).write(value2);
			
			indexRegister.write(index);
		}		
	}
	
	final static class PairRegisterLimitedMemory implements PairRegister {
		
		@Override
		public int[] read(ConcurrentSystem system, ProcessInfo callerInfo) {
			while (true) {
				int index = system.getRegister(-1).read();
				int[] value = new int[2];
				value[0] = system.getRegister(index).read();
				value[1] = system.getRegister(index + 1).read();
				return value;
			}			
		}
		
		@Override
		public void write(int value1, int value2, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register indexRegister = system.getCASRegister(-1);
			int freeSpaceIndex = callerInfo.getThreadLocal(0);
			freeSpaceIndex++;
			callerInfo.putThreadLocal(0, freeSpaceIndex + 1);
			
			int index = 2*callerInfo.getCurrentId() + (2*callerInfo.getTotalProcesses()) * freeSpaceIndex;
			system.getRegister(index).write(value1);
			system.getRegister(index+1).write(value2);
			
			indexRegister.write(index);
		}		
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		PairRegisterTester.testPairRegister(new PairRegisterNaive());
		System.out.println("Transaction:");
		PairRegisterTester.testPairRegister(new PairRegisterTransaction());
		System.out.println("Correct:");
		PairRegisterTester.testPairRegister(new PairRegisterCorrect());
		System.out.println("Wait-free:");
		PairRegisterTester.testPairRegister(new PairRegisterWaitFree());
	}
}
