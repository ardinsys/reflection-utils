package eu.ardinsys.reflection.tool;

/**
 * An interface which describes anything which can be used to instantiate classes.
 * <p>
 * See {@link #provideInstance(Class)}, {@link #getCompositeSize()}, {@link #setCompositeSize(int)}.
 */
public interface InstanceProvider {
	/**
	 * Provides an instance of the given class.
	 * 
	 * @param c
	 *          The class to instantiate
	 * @return An instance of <b>c</b>
	 */
	<T> T provideInstance(Class<T> c);

	/**
	 * Returns the size for the newly allocated composites (arrays, collections, maps).
	 * 
	 * @return The size
	 */
	int getCompositeSize();

	/**
	 * Sets the size for the newly allocated composites (arrays, collections, maps).
	 * 
	 * @param compositeSize
	 *          The size
	 */
	void setCompositeSize(int compositeSize);
}
