/**
 *
 */
package org.arachna.netweaver.pmd;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

/**
 * Wrapper for the Ant CPD task. Sets up the task and executes it.
 * 
 * @author Dirk Weigenand
 */
final class CpdBuildFileGenerator {
    /**
     * Helper class for setting up an ant task with class path, source file sets etc.
     */
    private final AntHelper antHelper;

    /**
     * The template engine to use for build file generation.
     */
    private final VelocityEngine engine;

    /**
     * Excludes for filesets.
     */
    private final Set<String> excludes = new HashSet<String>();

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
     * Create an executor for executing the CPD ant task using the given ant helper object.
     * 
     * @param buildFilePath
     *            path to build file
     * @param antHelper
     *            helper for populating an ant task with source filesets and class path for a given development component
     * @param engine
     *            template engine to use for generating build files.
     * @param encoding
     *            encoding to use for CPD
     * @param minimumTokenCount
     *            the minimal token count for CPD analysis.
     */
    CpdBuildFileGenerator(final String buildFilePath, final AntHelper antHelper, final VelocityEngine engine, final String encoding,
        final String minimumTokenCount) {
        this.buildFilePath = buildFilePath;
        this.antHelper = antHelper;
        this.engine = engine;
        this.encoding = encoding;
        this.minimumTokenCount = minimumTokenCount;
    }

    /**
     * Run the Ant CPD task for the given development component.
     * 
     * @param components
     *            development components to run CPD on.
     */
    public void execute(final Collection<DevelopmentComponent> components) {
        final Collection<CpdSourceFolderDescriptor> sources = getSourcePaths(components);

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
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Evaluate the Velocity context with the given collection of source directories and write it into the given writer.
     * 
     * @param sources
     *            collection of source folders
     * @param writer
     *            {@link Writer} to receive the transformed template.
     * @throws IOException
     *             when writing fails
     */
    protected void evaluateContext(final Collection<CpdSourceFolderDescriptor> sources, final Writer writer) throws IOException {
        final Context context = createVelocityContext(sources);
        engine.evaluate(context, writer, "cpd-all", getTemplateReader());
    }

    /**
     * @param sources
     * @return
     */
    protected Context createVelocityContext(final Collection<CpdSourceFolderDescriptor> sources) {
        final Context context = new VelocityContext();
        context.put("sourcePaths", sources);
        context.put("encoding", encoding);
        context.put("outputFile", String.format("%s/cpd/cpd-result.xml", antHelper.getPathToWorkspace()));
        context.put("minimumTokenCount", minimumTokenCount);

        return context;
    }

    /**
     * @param components
     */
    protected Collection<CpdSourceFolderDescriptor> getSourcePaths(final Collection<DevelopmentComponent> components) {
        final Collection<CpdSourceFolderDescriptor> sources = new LinkedList<CpdSourceFolderDescriptor>();
        final ExcludesFactory excludesFactory = new ExcludesFactory();

        for (final DevelopmentComponent component : components) {
            final Collection<String> sourceFolders = antHelper.createSourceFileSets(component);
            sourceFolders.addAll(component.getTestSourceFolders());

            for (final String folder : sourceFolders) {
                sources.add(new CpdSourceFolderDescriptor(folder, Arrays.asList(excludesFactory.create(component, excludes)), Arrays
                    .asList(excludesFactory.createContainsRegexpExcludes(component, excludes))));
            }
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
