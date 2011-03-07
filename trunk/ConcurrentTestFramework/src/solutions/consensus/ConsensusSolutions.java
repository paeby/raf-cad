package solutions.consensus;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.consensus.Consensus;

public class ConsensusSolutions {
	
	public static class ConsensusMutex implements Consensus {
		@Override
		public int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register register = system.getRegister(0);
			Mutex mutex = new Mutex(-1);
			
			mutex.lock(system, callerInfo);
			
			int valueInReg = register.read();
			if (valueInReg == 0) {
				valueInReg = value + 1;
				register.write(valueInReg);
			}
			
			mutex.unlock(system, callerInfo);
			
			return valueInReg - 1;
		}
		
		private final static class Mutex {
			final int registerIndex;
			
			public Mutex(int registerIndex) {
				super();
				this.registerIndex = registerIndex;
			}
			
			public void lock(ConcurrentSystem system, ProcessInfo info) {
				CASRegister reg = system.getCASRegister(registerIndex);
				while (!reg.compareAndSet(0, 1))
					;
			}
			
			public void unlock(ConcurrentSystem system, ProcessInfo info) {
				CASRegister reg = system.getCASRegister(registerIndex);
				reg.write(0);
			};
		}
	}
	
	public static class ConsensusWaitFree implements Consensus {
		final int registerIndex;
		
		public ConsensusWaitFree(int registerIndex) {
			this.registerIndex = registerIndex;
		}
		
		@Override
		public int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			if (reg.compareAndSet(0, value + 1))
				return value;
			else
				return reg.read() - 1;
			// +1 and -1 are only to allow values to be in [0,MAX_VALUE]
			// interval, without them, it works only for proposing [1,MAX_VALUE]
		}
		
		public int get(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			return reg.read() - 1;
		}
	}
	
	public static class ConsensusWaitFreePerformant implements Consensus {
		final int registerIndex;
		
		public ConsensusWaitFreePerformant(int registerIndex) {
			this.registerIndex = registerIndex;
		}
		
		@Override
		public int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			int oldVal = reg.read();
			if (oldVal > 0)
				return oldVal - 1;
			if (reg.compareAndSet(0, value + 1))
				return value;
			else
				return reg.read() - 1;
			// +1 and -1 are only to allow values to be in [0,MAX_VALUE]
			// interval, without them, it works only for proposing [1,MAX_VALUE]
		}
		
		public int get(ConcurrentSystem system, ProcessInfo callerInfo) {
			CASRegister reg = system.getCASRegister(registerIndex);
			return reg.read() - 1;
		}
	}
	
}
