package solutions.consensus;

import solutions.mutex.MutexSolutions.MutexCorrect;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.consensus.Consensus;
import examples.consensus.ConsensusTester;
import examples.mutex.Mutex;

public class ConsensusSolutions {
	
	public static class ConsensusNaive implements Consensus {
		@Override
		public int propose(int value, ConcurrentSystem system,
				ProcessInfo callerInfo) {
			Register reg = system.getRegister(0);
			int read = reg.read();
			if (read != 0)
				return read - 1;
			else {
				reg.write(value + 1);
				return value;
			}
		}
	}
	
	public static class ConsensusMutex implements Consensus {
		@Override
		public int propose(int value, ConcurrentSystem system, ProcessInfo callerInfo) {
			Register register = system.getRegister(0);
			Mutex mutex = new MutexCorrect(-1);
			
			mutex.lock(system, callerInfo);
			
			int valueInReg = register.read();
			if (valueInReg == 0) {
				valueInReg = value + 1;
				register.write(valueInReg);
			}
			
			mutex.unlock(system, callerInfo);
			
			return valueInReg - 1;
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
	
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		ConsensusTester.testConsensus(new ConsensusNaive());
		System.out.println("Mutex:");
		ConsensusTester.testConsensus(new ConsensusMutex());
		System.out.println("Wait-free:");
		ConsensusTester.testConsensus(new ConsensusWaitFree(0));
		System.out.println("Wait-free-perf:");
		ConsensusTester.testConsensus(new ConsensusWaitFreePerformant(0));
	}
	
}
