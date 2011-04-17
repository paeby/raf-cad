package common.problem;

public interface RandomizableProblemInstance<T extends Solution> extends ProblemInstance<T> {
	void randomize();
}
