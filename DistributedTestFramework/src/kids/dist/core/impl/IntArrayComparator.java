package kids.dist.core.impl;

import java.util.Comparator;

public class IntArrayComparator implements Comparator<int[]> {
	@Override
	public int compare(int[] a, int[] a2) {
        if (a==a2)
            return 0;

        int length = a.length;
        if (a2.length != length)
            return length - a2.length;

        for (int i=0; i<length; i++)
            if (a[i] != a2[i])
                return a[i] - a2[i];

        return 0;
	}
}