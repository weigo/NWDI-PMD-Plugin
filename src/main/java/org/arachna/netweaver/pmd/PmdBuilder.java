package org.arachna.netweaver.pmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.json.JSONObject;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.Compartment;
import org.arachna.netweaver.dc.types.CompartmentState;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.IDevelopmentComponentFilter;
import org.arachna.netweaver.hudson.nwdi.AntTaskBuilder;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 * 
 * <p>
 * When the user configures the project and enables this builder, {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link PmdBuilder} is created. The created instance is persisted to the project configuration XML by using XStream, so this allows you to
 * use instance fields (like {@link #name}) to remember the configuration.
 * 
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be invoked.
 * 
 * @author Kohsuke Kawaguchi
 */
public class PmdBuilder extends AntTaskBuilder {
    /**
     * Descriptor for {@link PmdBuilder}.
     */
    @Extension(ordinal = 1000)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Indicates that CPD should be run if set to <code>true</code>.
     */
    private final boolean runCpd;

    /**
     * Create a new PmdBuilder instance.
     * 
     * @param runCpd
     *            indicate whether CPD should be run (<code>true</code>) or not (<code>false</code>).
     */
    @DataBoundConstructor
    public PmdBuilder(final boolean runCpd) {
        this.runCpd = runCpd;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getRunCpd() {
        return runCpd;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        boolean result = true;

        final NWDIBuild nwdiBuild = (NWDIBuild)build;

        if (runCpd) {
            result = runCpd(launcher, listener, nwdiBuild);
        }

        return result;
    }

    /**
     * @param launcher
     * @param listener
     * @param result
     * @param nwdiBuild
     * @param antHelper
     * @param filter
     * @return
     */
    protected boolean runCpd(final Launcher launcher, final BuildListener listener, final NWDIBuild nwdiBuild) {
        boolean result = true;
        final long start = System.currentTimeMillis();
        final AntHelper antHelper = getAntHelper();
        final File cpdFolder = new File(String.format("%s/cpd", antHelper.getPathToWorkspace()));

        if (!cpdFolder.exists() && !cpdFolder.mkdirs()) {
            listener.getLogger().append(String.format("Can't create CPD result folder ('%s')!", cpdFolder.getAbsolutePath()));
            result = false;
        }

        listener.getLogger().append("Running CPD...");

        final String buildFileName = String.format("%s/cpd-build.xml", antHelper.getPathToWorkspace());

        // FIXME: make encoding and minimumTokenCount configurable!
        final CpdBuildFileGenerator generator = new CpdBuildFileGenerator(buildFileName, antHelper, getVelocityEngine(), "UTF-8", "100");
        final Collection<Compartment> compartments = nwdiBuild.getDevelopmentConfiguration().getCompartments(CompartmentState.Source);
        generator.execute(getDevelopmentComponentsWithJavaSources(compartments));

        result = execute(nwdiBuild, launcher, listener, "cpd-all", buildFileName, "-Xmx1024m");

        listener.getLogger().append(String.format("(%f sec.).\n", (System.currentTimeMillis() - start) / 1000f));
        return result;
    }

    /**
     * @param compartments
     * @return
     */
    protected Collection<DevelopmentComponent> getDevelopmentComponentsWithJavaSources(final Collection<Compartment> compartments) {
        final Collection<DevelopmentComponent> components = new ArrayList<DevelopmentComponent>();
        final IDevelopmentComponentFilter filter = new DCWithJavaSourceAcceptingFilter();

        for (final Compartment compartment : compartments) {
            components.addAll(compartment.getDevelopmentComponents(filter));
        }

        return components;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor for {@link PmdBuilder}. Used as a singleton. The class is marked as public so that it can be accessed from views.
     * 
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment for the configuration screen.
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Will return <code>true</code> when the given project is of type {@link NWDIProject}, <code>false</code> otherwise.
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return NWDIProject.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "NWDI PMD Builder";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            // ^Can also use req.bindJSON(this, formData);
            // (easier when there are many fields; need set* methods for this,
            // like setUseFrench)
            save();
            return super.configure(req, formData);
        }
    }

    /**
     * Get the properties to use calling ant.
     * 
     * @return the properties to use calling ant.
     */
    @Override
    protected String getAntProperties() {
        return String.format("cpd.dir=%s/plugins/NWDI-PMD-Plugin/WEB-INF/lib",
            Hudson.getInstance().root.getAbsolutePath().replace("\\", "/"));
    }
}
