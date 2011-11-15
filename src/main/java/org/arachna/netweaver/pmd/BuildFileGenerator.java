/**
 *
 */
package org.arachna.netweaver.pmd;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;

/**
 * Wrapper for the Ant JavaDoc task. Sets up the task and executes it.
 * 
 * @author Dirk Weigenand
 */
final class BuildFileGenerator {
    /**
     * Helper class for setting up an ant task with class path, source file sets
     * etc.
     */
    private final AntHelper antHelper;

    /**
     * Registry for development components.
     */
    private final DevelopmentComponentFactory dcFactory;

    /**
     * Development configuration to use generating the Java version for the
     * JavaDoc task.
     */
    private final DevelopmentConfiguration developmentConfiguration;

    /**
     * The template engine to use for build file generation.
     */
    private final VelocityEngine engine;

    /**
     * Paths to generated build files.
     */
    private final Collection<String> buildFilePaths = new HashSet<String>();

    /**
     * @return the buildFilePaths
     */
    final Collection<String> getBuildFilePaths() {
        return buildFilePaths;
    }

    /**
     * Create an executor for executing the CPD ant task using the given ant
     * helper object.
     * 
     * @param developmentConfiguration
     * 
     * @param antHelper
     *            helper for populating an ant task with source filesets and
     *            class path for a given development component
     * @param engine
     *            template engine to use for generating build files.
     */
    BuildFileGenerator(final DevelopmentConfiguration developmentConfiguration, final AntHelper antHelper,
        final DevelopmentComponentFactory dcFactory, final VelocityEngine engine) {
        this.developmentConfiguration = developmentConfiguration;
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.engine = engine;
    }

    /**
     * Run the Ant CPD task for the given development component.
     * 
     * @param component
     *            development component to document with JavaDoc.
     */
    public void execute(final Collection<DevelopmentComponent> components) {
        final HashSet<String> excludes = new HashSet<String>();
        final Collection<String> sources = new HashSet<String>();
        final Collection<String> classpaths = new HashSet<String>();

        for (final DevelopmentComponent component : components) {
            sources.addAll(antHelper.createSourceFileSets(component, excludes, excludes));
            classpaths.addAll(antHelper.createClassPath(component));
        }

        if (!sources.isEmpty()) {
            final Context context = new VelocityContext();
            context.put("sourcePaths", sources);
            context.put("classpaths", classpaths);
            context.put("encoding", "UTF-8");
            context.put("outputFile", String.format("%s/cpd-result.xml", antHelper.getPathToWorkspace()));
            context.put("excludes", excludes);
            context.put("excludeContainsRegexps", excludes);

            final String location = String.format("%s/cpd-build.xml", antHelper.getPathToWorkspace());
            Writer buildFile = null;

            try {
                buildFile = new FileWriter(location);
                engine.evaluate(context, buildFile, "cpd-all", getTemplateReader());
                buildFilePaths.add(location);
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                if (buildFile != null) {
                    try {
                        buildFile.close();
                    }
                    catch (final IOException e) {
                        // TODO Auto-generated catch block
                        // e.printStackTrace(logger);
                    }
                }
            }
        }
    }

    /**
     * @return
     */
    private Reader getTemplateReader() {
        return new InputStreamReader(this.getClass().getResourceAsStream("/org/arachna/netweaver/pmd/cpd-build.vm"));
    }
}
