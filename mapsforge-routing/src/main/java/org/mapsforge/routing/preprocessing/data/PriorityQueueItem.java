package org.mapsforge.routing.preprocessing.data;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
// TODO: documentation
public class PriorityQueueItem<E extends Comparable<E>> implements IPriorityQueueItem<E> {
    /**
     * The priority.
     */
    private E priority;

    /* (non-JavaDoc)
     * @see IPriorityQueueItem#getPriority()
     */
    @Override
    public E getPriority() {
        return priority;
    }

    /* (non-JavaDoc)
     * @see IPriorityQueueItem#setPriority(E)
     */
    @Override
    public void setPriority(E priority) {
        this.priority = priority;
    }

    /* (non-JavaDoc)
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo(IPriorityQueueItem<E> o) {
        int result;
        if (o == null) {
            result = -1;
        }
        else {
            E priority1 = getPriority();
            E priority2 = o.getPriority();

            if (priority1 == null && priority2 == null) {
                result = 0;
            }
            else if (priority1 == null) {
                result = 1;
            }
            else if (priority2 == null) {
                result = -1;
            }
            else {
                result = priority1.compareTo(priority2);
            }
        }

        return result;
    }
}
