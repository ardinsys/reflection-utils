package eu.ardinsys.reflection.tool;

import java.util.Set;

/**
 * An interface which describes anything which can be used to provide implementations for abstract classes or
 * interfaces.
 * <p>
 * See {@link #provideImplementations(Class)}.
 */
public interface ImplementationProvider {
	/**
	 * Returns a list of implementations for a superclass.
	 * 
	 * @param superClass
	 *          The superclass
	 * @return A list of implementations
	 */
	Set<Class<?>> provideImplementations(Class<?> superClass);
}
