package org.mapsforge.routing.ch.preprocessing.graph;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
// TODO: documentation
public interface ShortcutPath {

    public CHEdge[] getPath();
    public CHEdge[] getUsedShortcuts();

}
