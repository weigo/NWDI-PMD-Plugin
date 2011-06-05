/**
 *
 */
package org.arachna.netweaver.pmd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.pmd.cpd.CPDTask;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludeDataDictionarySourceDirectoryFilter;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

/**
 * Executor for CPD (Copy Paste Detector).
 *
 * @author Dirk Weigenand
 */
public final class CpdExecutor {
    /**
     * Collection of development components
     */
    private final Collection<DevelopmentComponent> components = new ArrayList<DevelopmentComponent>();

    /**
     * helper class for ant tasks.
     */
    private final AntHelper antHelper;

    /**
     * Create an instance of <code>CpdExecutor</code> using the given
     * {@link AntHelper} and development components.
     *
     * @param antHelper
     *            helper class for preparing ant tasks for execution.
     * @param components
     *            development components to check for Copy and Pasted source
     *            code.
     */
    public CpdExecutor(final AntHelper antHelper, Collection<DevelopmentComponent> components) {
        this.antHelper = antHelper;

        if (components != null) {
            this.components.addAll(components);
        }
    }

    public void execute() {
        // FIXME: Use global excludes, i.e. exclude generated sources from CPD
        // analysis
        final Set<String> excludes = new HashSet<String>();
        final CPDTask task = new CPDTask();
        final Project project = new Project();

        task.setProject(project);

        for (final DevelopmentComponent component : components) {
            ExcludeDataDictionarySourceDirectoryFilter excludeDataDictionarySources =
                new ExcludeDataDictionarySourceDirectoryFilter();

            for (final FileSet fileSet : antHelper.createSourceFileSets(component, excludeDataDictionarySources,
                excludes, excludes)) {
                fileSet.setProject(project);
                task.addFileset(fileSet);
            }
        }

        // FIXME: make encoding configurable!
        task.setEncoding("UTF-8");

        final CPDTask.FormatAttribute format = new CPDTask.FormatAttribute();
        format.setValue("xml");
        task.setFormat(format);

        final CPDTask.LanguageAttribute language = new CPDTask.LanguageAttribute();
        language.setValue("java");

        // FIXME: Make minimumTokenCount configurable!
        task.setMinimumTokenCount(50);

        task.setOutputFile(new File(String.format("%s/cpd/cpd.xml", antHelper.getPathToWorkspace())));
        task.execute();
    }
}
