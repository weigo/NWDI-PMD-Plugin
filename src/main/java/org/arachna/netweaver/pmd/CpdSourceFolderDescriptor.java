/**
 * 
 */
package org.arachna.netweaver.pmd;

import java.util.Collection;
import java.util.HashSet;

/**
 * Descriptor for the generation of filesets for source code analysis with CPD.
 * 
 * @author Dirk Weigenand
 */
public final class CpdSourceFolderDescriptor {
    /**
     * Source folder.
     */
    private final String sourceFolder;

    /**
     * Excludes to use via file name pattern.
     */
    private final Collection<String> excludes = new HashSet<String>();

    /**
     * Excludes to use via containing regexp pattern.
     */
    private final Collection<String> containsRegexpExcludes = new HashSet<String>();

    /**
     * Create a new <code>CpdSourceFolderDescriptor</code> using the given
     * source folder and set of excludes.
     * 
     * @param sourceFolder
     * @param excludes
     * @param containsRegexpExcludes
     */
    CpdSourceFolderDescriptor(String sourceFolder, Collection<String> excludes,
        Collection<String> containsRegexpExcludes) {
        this.sourceFolder = sourceFolder;

        if (excludes != null) {
            this.excludes.addAll(excludes);
        }

        if (containsRegexpExcludes != null) {
            this.containsRegexpExcludes.addAll(containsRegexpExcludes);
        }
    }

    /**
     * @return the sourceFolder
     */
    public final String getSourceFolder() {
        return sourceFolder;
    }

    /**
     * @return the excludes
     */
    public final Collection<String> getExcludes() {
        return excludes;
    }

    /**
     * @return the containsRegexpExcludes
     */
    public final Collection<String> getContainsRegexpExcludes() {
        return containsRegexpExcludes;
    }
}
