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

/**
 * Wrapper for the Ant CPD task. Sets up the task and executes it.
 * 
 * @author Dirk Weigenand
 */
final class CpdBuildFileGenerator {
    /**
     * Helper class for setting up an ant task with class path, source file sets
     * etc.
     */
    private final AntHelper antHelper;

    /**
     * The template engine to use for build file generation.
     */
    private final VelocityEngine engine;

    /**
     * Excludes for filesets.
     */
    private final HashSet<String> excludes = new HashSet<String>();

    /**
     * Path to generated build file.
     */
    private final String buildFilePath;

    /**
     * encoding to use for CPD.
     */
    private final String encoding;

    /**
     * the minimal token count for CPD analysis.
     */
    private final String minimumTokenCount;

    /**
     * Create an executor for executing the CPD ant task using the given ant
     * helper object.
     * 
     * @param buildFilePath
     *            path to build file
     * @param antHelper
     *            helper for populating an ant task with source filesets and
     *            class path for a given development component
     * @param engine
     *            template engine to use for generating build files.
     * @param encoding
     *            encoding to use for CPD
     * @param minimumTokenCount
     *            the minimal token count for CPD analysis.
     */
    CpdBuildFileGenerator(final String buildFilePath, final AntHelper antHelper, final VelocityEngine engine,
        final String encoding, final String minimumTokenCount) {
        this.buildFilePath = buildFilePath;
        this.antHelper = antHelper;
        this.engine = engine;
        this.encoding = encoding;
        this.minimumTokenCount = minimumTokenCount;
    }

    /**
     * Run the Ant CPD task for the given development component.
     * 
     * @param component
     *            development component to document with JavaDoc.
     */
    public void execute(final Collection<DevelopmentComponent> components) {
        final Collection<String> sources = getSourcePaths(components);

        if (!sources.isEmpty()) {
            Writer buildFile = null;

            try {
                buildFile = new FileWriter(buildFilePath);
                evaluateContext(sources, buildFile);
            }
            catch (final IOException e) {
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
     * Evaluate the Velocity context with the given collection of source
     * directories and write it into the given writer.
     * 
     * @param sources
     *            collection of source folders
     * @param writer
     *            {@link Writer} to receive the transformed template.
     * @throws IOException
     *             when writing fails
     */
    protected void evaluateContext(final Collection<String> sources, final Writer writer) throws IOException {
        final Context context = createVelocityContext(sources);
        engine.evaluate(context, writer, "cpd-all", getTemplateReader());
    }

    /**
     * @param sources
     * @return
     */
    protected Context createVelocityContext(final Collection<String> sources) {
        final Context context = new VelocityContext();
        context.put("sourcePaths", sources);
        context.put("encoding", encoding);
        context.put("outputFile", String.format("%s/cpd/cpd-result.xml", antHelper.getPathToWorkspace()));
        context.put("excludes", excludes);
        context.put("excludeContainsRegexps", excludes);
        context.put("minimumTokenCount", minimumTokenCount);

        return context;
    }

    /**
     * @param components
     */
    protected Collection<String> getSourcePaths(final Collection<DevelopmentComponent> components) {
        final Collection<String> sources = new HashSet<String>();

        for (final DevelopmentComponent component : components) {
            sources.addAll(antHelper.createSourceFileSets(component, excludes, excludes));
        }

        return sources;
    }

    /**
     * @return
     */
    private Reader getTemplateReader() {
        return new InputStreamReader(this.getClass().getResourceAsStream("/org/arachna/netweaver/pmd/cpd-build.vtl"));
    }
}
