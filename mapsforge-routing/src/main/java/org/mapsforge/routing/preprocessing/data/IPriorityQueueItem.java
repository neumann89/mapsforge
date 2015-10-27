package org.mapsforge.routing.preprocessing.data;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
// TODO: documentation
public interface IPriorityQueueItem<E extends Comparable<E>> extends Comparable<IPriorityQueueItem<E>> {

    /**
     * Returns this item's priority.
     *
     * @return This item's priority.
     */
    public E getPriority();

    /**
     * Sets a new value for this items priority.
     * @param priority
     *      The new priority of this item.
     */
    public void setPriority(E priority);
}
