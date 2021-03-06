/**
 * 
 */
package org.arachna.netweaver.pmd;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.velocity.app.VelocityEngine;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dirk Weigenand
 * 
 */
public class CpdBuildFileGeneratorTest {
    /**
     * Instance under test.
     */
    private CpdBuildFileGenerator generator;

    private DevelopmentComponentFactory dcFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        dcFactory = new DevelopmentComponentFactory();
        dcFactory.create("example.com", "dc1", DevelopmentComponentType.WebDynpro);
        final AntHelper antHelper = new AntHelper("", dcFactory);

        generator = new CpdBuildFileGenerator("cpd-build.xml", antHelper, new VelocityEngine(), "UTF-8", "50");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        generator = null;
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.pmd.CpdBuildFileGenerator#evaluateContext(java.util.Collection, java.io.Writer)}
     * .
     * 
     * @throws IOException
     *             when transforming or reading the template fails
     */
    @Test
    public final void testEvaluateContext() throws IOException {
        final StringWriter writer = new StringWriter();
        final Collection<CpdSourceFolderDescriptor> sources = new HashSet<CpdSourceFolderDescriptor>();
        DevelopmentComponent dc = this.dcFactory.get("example.com", "dc1");
        ExcludesFactory excludesFactory = new ExcludesFactory();
        sources.add(new CpdSourceFolderDescriptor(".dtc/DCs/example.org/dc1/src/packages", Arrays
            .asList(excludesFactory.create(dc, new HashSet<String>())), Arrays.asList(excludesFactory
            .createContainsRegexpExcludes(dc, new HashSet<String>()))));

        generator.evaluateContext(sources, writer);

        System.err.println(writer.toString());
    }
}
