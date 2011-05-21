package kids.dist.util;

import java.util.Arrays;

public class RandomIntGenerator {
	public static int[] generateDifferentInts(int mod, int count) {
		if (count > mod)
			throw new IllegalArgumentException("Cannot create " + count + " different numbers of mod " + mod);
		int[] results = new int[count];
		for (int i = 0; i < count; i++) {
			int num = (int) (Math.random() * (mod - i));
			int smallerOnes, oldSmallerOnes = 0;
			while (true) {
				smallerOnes = 0;
				for (int j = 0; j < i; j++)
					if (results[j] <= num + oldSmallerOnes)
						smallerOnes++;
				if (smallerOnes != oldSmallerOnes)
					oldSmallerOnes = smallerOnes;
				else
					break;
			}
			results[i] = num + smallerOnes;
		}
		return results;
	}
	
	public static void main(String[] args) {
		System.out.println(Arrays.toString(generateDifferentInts(5, 5)));
	}
}
