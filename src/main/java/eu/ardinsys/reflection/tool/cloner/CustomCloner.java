package eu.ardinsys.reflection.tool.cloner;

/**
 * Interface for custom type cloner implementations.
 * <p>
 * See {@link #clone(Object, Class)}.
 *
 * @param <T>
 *          The type of the object to clone
 * @param <U>
 *          The type of the cloned object
 */
public interface CustomCloner<T, U> {
	/**
	 * Clones the object.
	 *
	 * @param object
	 *          The object to clone
	 * @param targetClass
	 *          The runtime class of the target object
	 * @return The cloned <b>object</b>
	 */
	U clone(T object, Class<? extends U> targetClass);
}
