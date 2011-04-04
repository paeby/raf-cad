package examples.copyonwritearray;

import useful.ObjectHelper;

public class CopyOnWriteArrayTester {
	
	public static void testCopyOnWriteArray(CopyOnWriteArray array) {
		CopyOnWriteArrayProblemInstance instance = null;
		try {
			instance = new CopyOnWriteArrayProblemInstance();
			
			for (int i = 0; i < 200; i++) {
				if (i % 10 == 0)
					System.out.print('.');
				if (!instance.testOnce(ObjectHelper.copy(array), 200, 1))
					return;
			}
			System.out.println();
			
			for (int i = 0; i < 200; i++) {
				if (i % 10 == 0)
					System.out.print('.');
				if (!instance.testOnce(ObjectHelper.copy(array), 200, 10))
					return;
			}
			System.out.println();
			System.out.println("All good!");
		} finally {
			if (instance != null)
				instance.shutdownExecutor();
		}
	}
}
