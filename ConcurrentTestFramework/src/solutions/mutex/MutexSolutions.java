package solutions.mutex;

import common.ConcurrentSystem;
import common.ProcessInfo;
import common.registers.CASRegister;
import common.registers.Register;

import examples.mutex.Mutex;
import examples.mutex.MutexTester;

public class MutexSolutions {
	private static final class MutexNaive implements Mutex {
		@Override
		public void lock(ConcurrentSystem system, ProcessInfo info) {}
		
		@Override
		public void unlock(ConcurrentSystem system, ProcessInfo info) {};
	}
	
	private static final class MutexCorrect implements Mutex {
		@Override
		public void lock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			while (!reg.compareAndSet(0, 1));
		}
		
		@Override
		public void unlock(ConcurrentSystem system, ProcessInfo info) {
			CASRegister reg = system.getCASRegister(0);
			reg.write(0);
		};
	}
	
	private static final class MutexReadWrite implements Mutex {
		@Override
		public void lock(ConcurrentSystem system, ProcessInfo info) {
			Register reg = system.getRegister(info.getCurrentId());
			
			Register flag = system.getRegister(info.getCurrentId() + info.getTotalProcesses());
			flag.write(1);
			
			int max = 0;
			for(int i = 0;i<info.getTotalProcesses();i++)
				max = Math.max(max, system.getRegister(i).read());
			
			max++;
			reg.write(max);
			flag.write(0);
			
			for(int i = 0;i<info.getTotalProcesses();i++)
				if (i != info.getCurrentId()) {
					while (true) {
						if (system.getRegister(i+ info.getTotalProcesses()).read()==0)
							break;
					}
					
					while (true) {
						int cur = system.getRegister(i).read();
						if (cur == 0 || (cur > max) || (cur == max && i > info.getCurrentId()))
							break;
					}
				}
				
			return;
		}
		
		@Override
		public void unlock(ConcurrentSystem system, ProcessInfo info) {
			Register reg = system.getRegister(info.getCurrentId());
			reg.write(0);			
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Naive:");
		MutexTester.testMutex(new MutexNaive());
		System.out.println("Correct:");
		MutexTester.testMutex(new MutexCorrect());
		System.out.println("Read/write:");
		MutexTester.testMutex(new MutexReadWrite());
	}
}
