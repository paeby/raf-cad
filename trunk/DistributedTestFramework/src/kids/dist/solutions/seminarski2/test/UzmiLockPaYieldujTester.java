package kids.dist.solutions.seminarski2.test;

import java.util.concurrent.atomic.AtomicInteger;

import kids.dist.common.problem.Solution;
import kids.dist.core.DistributedManagedSystem;
import kids.dist.core.impl.problem.DefaultProblemInstance;
import kids.dist.core.impl.problem.ProblemTester;
import kids.dist.core.impl.problem.SingleProcessTester;
import kids.dist.core.impl.problem.TesterVerdict;
import kids.dist.core.network.CliqueDistNetwork;

public class UzmiLockPaYieldujTester extends DefaultProblemInstance<UzmiLockPaYieldujTester.NothingSolution> {
	
	AtomicInteger integer = new AtomicInteger(0);
	
	public SingleProcessTester<NothingSolution> createSingleProcessTester(kids.dist.core.DistributedManagedSystem system, NothingSolution mySolution, int threadIndex) {
		return new SingleProcessTester<UzmiLockPaYieldujTester.NothingSolution>() {
			
			@Override
			public TesterVerdict test(DistributedManagedSystem system, NothingSolution solution) {
				for (int i = 0; i < 1000; i++) {
					if (integer.getAndIncrement() != 0)
						return TesterVerdict.FAIL;
					if (integer.decrementAndGet() != 0)
						return TesterVerdict.FAIL;
					system.yield();
				}
				
				return TesterVerdict.SUCCESS;
			}
		};
	};
	
	public static class NothingSolution implements Solution {
		@Override
		public void messageReceived(int from, int type, Object message) {

		}
	}
	
	public static void main(String[] args) {
		ProblemTester.testProblem(new UzmiLockPaYieldujTester(), NothingSolution.class, new CliqueDistNetwork.Factory(5), 100);
	}
}
