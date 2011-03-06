package solutions.casn;

import common.ConcurrentSystem;
import common.ProcessInfo;
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
			
			int v1 = reg1.read();
			int v2 = reg2.read();
			if (v1 == expected1 && v2 == expected2) {
				reg1.write(update1);
				reg2.write(update2);
				return true;
			} else {
				return false;
			}
		}
		
	}
}
