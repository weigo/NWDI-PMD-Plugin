package org.arachna.netweaver.pmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
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
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.IDevelopmentComponentFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link PmdBuilder} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class PmdBuilder extends Builder {

    private final boolean runCpd;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
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
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final NWDIBuild nwdiBuild = (NWDIBuild)build;
        final AntHelper antHelper = nwdiBuild.getAntHelper(listener.getLogger());
        final File cpdFolder = new File(String.format("%s/cpd", antHelper.getPathToWorkspace()));

        if (!cpdFolder.exists() && !cpdFolder.mkdirs()) {
            listener.getLogger().append(
                String.format("Can't create CPD result folder ('%s')!", cpdFolder.getAbsolutePath()));
            return false;
        }

        IDevelopmentComponentFilter filter = new DCWithJavaSourceAcceptingFilter();

        if (runCpd) {
            long start = System.currentTimeMillis();
            listener.getLogger().append("Running CPD...");

            final Collection<DevelopmentComponent> components = new ArrayList<DevelopmentComponent>();

            for (Compartment compartment : nwdiBuild.getDevelopmentConfiguration().getCompartments(
                CompartmentState.Source)) {
                for (DevelopmentComponent component : compartment.getDevelopmentComponents()) {
                    if (filter.accept(component)) {
                        components.add(component);
                    }
                }
            }

            final CpdExecutor executor = new CpdExecutor(antHelper, components);
            executor.execute();
            listener.getLogger().append(String.format("(%f sec.).\n", (System.currentTimeMillis() - start) / 1000f));
        }

        for (final DevelopmentComponent component : nwdiBuild.getAffectedDevelopmentComponents(filter)) {

        }

        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link PmdBuilder}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    // this marker indicates Hudson that this is an implementation of an
    // extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project
            // types
            return NWDIProject.class.equals(aClass.getClass());
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
}
