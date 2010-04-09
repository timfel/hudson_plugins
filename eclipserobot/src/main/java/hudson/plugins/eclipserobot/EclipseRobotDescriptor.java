package hudson.plugins.eclipserobot;

import hudson.model.Descriptor;
import hudson.tasks.Builder;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for EclipseRobot.
 * 
 * @author Marco Ambu
 */
public final class EclipseRobotDescriptor extends Descriptor<Builder> {

    public EclipseRobotDescriptor() {
        super(EclipseRobotBuilder.class);
        load();
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject formData) {
        save();
        return true;
    }

    @Override
    public String getHelpFile() {
        return "/plugin/eclipserobot/help.html";
    }

    @Override
    public String getDisplayName() {
        return Messages.EclipseRobot_DisplayName();
    }

    @Override
    public EclipseRobotBuilder newInstance(final StaplerRequest req, final JSONObject formData) throws FormException {
        return req.bindJSON(EclipseRobotBuilder.class, formData);
    }

}
