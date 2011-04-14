package core.impl.problem;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Utils;
import common.problem.ProblemInstance;
import common.problem.Solution;

import core.impl.DistributedManagedSystemImpl;
import core.impl.InstructionType;
import core.impl.NamedThreadFactory;
import core.network.RandomNetworkGenerator;

public class ProblemTester {
	
	public static <S extends Solution> boolean testProblem(ProblemInstance<S> problem, Class<? extends S> solutionClass, int nodeCount, int density, int n) {
		ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("workers"));
		int[][] graph = null;
		
		Map<InstructionType, Integer> stats = new TreeMap<InstructionType, Integer>();
		long sumTasks = 0;
		long sumSteps = 0;
		
		double numberOfDots = 20;
		double accumulatedDots = 0;
		for (int i = 0; i < n; i++) {
			if (graph == null || Math.random() < 0.1d) {
				graph = density == 100 ? RandomNetworkGenerator.generateCliqueInfos(nodeCount) : RandomNetworkGenerator.generateNeighbourhoodInfos(nodeCount, density);
			}
			DistributedManagedSystemImpl system = new DistributedManagedSystemImpl(executor, graph);
			boolean success = problem.execute(system, solutionClass);
			
			if (!success) {
				System.out.println();
				System.out.println("Bad state found");
				System.out.println("State 1 : ");
				system.printFinalState();
				return false;
			}
			sumTasks += system.getStartedTasks();
			sumSteps += system.getSteps();
			
			for (Entry<InstructionType, Integer> e : system.getStats().entrySet())
				Utils.add(stats, e.getKey(), e.getValue());
			
			accumulatedDots += numberOfDots/n;
			while (accumulatedDots >= 1) {
				System.out.print(".");
				accumulatedDots -= 1;
			}
			System.out.flush();
			System.gc();
		}
		System.out.println();
		System.out.println("Test passed : " + problem);
		System.out.print("Averages:\t");
		for (Entry<InstructionType, Integer> e : stats.entrySet())
			System.out.print(e.getKey().toString().toLowerCase() + ": " + (((double) e.getValue()) / n) + "\t");
		System.out.println("log lines: " + (((double) sumSteps) / n) + "\ttasks: " + (((double) sumTasks) / n));
		return true;
	}
	
}
