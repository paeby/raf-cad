package solutions.casn;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.casn.CompareAndSet2;

public class CompareAndSet2Solutions {
	final class CaS2Naive implements CompareAndSet2 {
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
		
		@Override
		public boolean compareAndSet(int expected1, int expected2, int update1, int update2, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register reg1 = system.getRegister(0);
			Register reg2 = system.getRegister(1);
			
			if (reg1.read() == expected1 && reg2.read() == expected2) {
				reg1.write(update1);
				reg2.write(update2);
				return true;
			} else {
				return false;
			}
		}
		
	}
	
	final class CaS2Correct implements CompareAndSet2 {
		
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
			
			int index;
			do {
				index = indexRegister.read();
			} while (!indexRegister.compareAndSet(index, freeSpaceIndex));
			
		}
		
		@Override
		public boolean compareAndSet(int expected1, int expected2, int update1, int update2, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister indexRegister = system.getCASRegister(-1);
			CASRegister freeSpaceIndexRegister = system.getCASRegister(-2);
			
			// prva provera, da li uopšte alocirati novu memoriju?
			int index = indexRegister.read();
			if (system.getRegister(index * 2).read() != expected1 || system.getRegister(index * 2 + 1).read() != expected2)
				return false;
			
			// okej, pokušavamo cas. Prvo ide zapis na novu lokaciju
			int freeSpaceIndex;
			do {
				freeSpaceIndex = freeSpaceIndexRegister.read();
			} while (!freeSpaceIndexRegister.compareAndSet(freeSpaceIndex, freeSpaceIndex + 1));
			
			system.getRegister(2 * freeSpaceIndex + 2).write(update1);
			system.getRegister(2 * freeSpaceIndex + 3).write(update2);
			
			while (!indexRegister.compareAndSet(index, freeSpaceIndex)) {
				// možda se index promenio, ali pokazuje na vrednosti koje
				// zadovoljavaju ovaj poziv cas2?
				index = indexRegister.read();
				if (system.getRegister(index * 2).read() != expected1 || system.getRegister(index * 2 + 1).read() != expected2)
					return false;
			}
			return true;
		}
		
	}
}
