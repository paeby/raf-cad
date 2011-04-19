package kids.dist.util;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectHelper {
	public static Method getMethod(Class<?> klass, String name, Class<?>... args) {
		while (klass != null) {
			nextMethod: for (Method method : klass.getDeclaredMethods())
				if (method.getName().equals(name) && method.getParameterTypes().length == args.length) {
					for (int i = 0; i < args.length; i++)
						if (!method.getParameterTypes()[i].isAssignableFrom(args[i]))
							continue nextMethod;
					return method;
				}
			klass = klass.getSuperclass();
		}
		return null;
	}
	
	private static final ConcurrentHashMap<Class<?>, Method> cloneMethods = new ConcurrentHashMap<Class<?>, Method>();
	
	@SuppressWarnings("unchecked")
	public static <T> T clone(T t) {
		if (t == null)
			return null;
		try {
			Method cloneMethod = cloneMethods.get(t.getClass());
			if (cloneMethod == null) {
				cloneMethod = getMethod(t.getClass(), "clone");
				cloneMethod.setAccessible(true);
				cloneMethods.put(t.getClass(), cloneMethod);
			}
			return (T) cloneMethod.invoke(t);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
