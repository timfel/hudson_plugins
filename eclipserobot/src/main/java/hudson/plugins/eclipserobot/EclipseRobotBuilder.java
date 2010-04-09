package hudson.plugins.eclipserobot;

import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;
import java.net.Socket;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * EclipseRobot Builder Plugin.
 *
 * @author Marco Ambu
 */
public final class EclipseRobotBuilder extends Builder {

	@Extension
	public static final EclipseRobotDescriptor DESCRIPTOR = new EclipseRobotDescriptor();

	/**
	 * Set to true for debugging.
	 */
	private static final boolean DEBUG = false;

	/**
	 * Command line.
	 */
	private final String commandLine;
	
	/**
	 * Eclipse executable
	 */
	private final String eclipseExecutable;

	/**
	 * Specify if command is executed from working dir.
	 */
	private final Boolean executeFromWorkingDir;

	public String getCommandLine() {
		return commandLine;
	}
	
	public String getEclipseExecutable() {
		return eclipseExecutable;
	}

	public Boolean getExecuteFromWorkingDir() {
		return executeFromWorkingDir;
	}
	
	@DataBoundConstructor
	public EclipseRobotBuilder(final String commandLine, final String eclipseExecutable, final Boolean executeFromWorkingDir) {
		this.commandLine = Util.fixEmptyAndTrim(commandLine);
		this.eclipseExecutable = Util.fixEmptyAndTrim(eclipseExecutable);
		this.executeFromWorkingDir = executeFromWorkingDir;
	}

	@Override
	public Descriptor<Builder> getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
	throws InterruptedException, IOException {

		ArgumentListBuilder args = new ArgumentListBuilder();
		final EnvVars env = build.getEnvironment(listener);
		if (commandLine != null && eclipseExecutable != null) {
			listener.getLogger().println("Performing Eclipse-Robot script task...");
			
			listener.getLogger().println("Running the following command:" + commandLine);
			listener.getLogger().println("Running the following eclipse:" + eclipseExecutable);

			listener.getLogger().println("Trying to run Eclipse...");
			String executable = eclipseExecutable;
			CommandInterpreter runner = getCommandInterpreter(launcher, executable + " -data . &");
			runner.perform(build, launcher, listener);

			listener.getLogger().println("Waiting for connection...");
			sendToServer("", listener.getLogger(), 4 * 60 * 1000); // Timeout after 4 mins
			listener.getLogger().println("Connection established...");

			listener.getLogger().println("Running script...");
			BufferedReader reader = new BufferedReader(new StringReader(commandLine));
			while (reader.ready()) {
				String readLine = reader.readLine();
				listener.getLogger().println("Sending data: [" + readLine + "]");
				if (!sendToServer(readLine, listener.getLogger(), 2000)) {
					listener.getLogger().println("Unable to send data, connection reset...");
					reader.close();
				}
			}
		}
		return true;
	}

	/**
	 * Try sending indefinitely.
	 * 
	 * {@inheritDoc #sendToServer(String, PrintStream, int)}
	 * @param string
	 * @param logger
	 */
	private boolean sendToServer(String string, PrintStream logger) {
		return sendToServer(string, logger, 0);
	}

	/**
	 * Send the specified string to the server. Log any returned data.
	 * @param string the data to be sent to the server
	 * @param logger a stream to log to
	 * @param timeout Timeout in milliseconds
	 * @return 
	 */
	private boolean sendToServer(String string, PrintStream logger, int timeout) {
		long time = System.currentTimeMillis();
		Socket s = null;

		// Try establishing a connection
		while (s == null && (timeout == 0 || System.currentTimeMillis() - time < timeout)) {
			try {
				s = new Socket(getHost(), getPort());
			} catch (IOException e) {
				s = null;
				logger.println("Connection attempt failed, retrying...");
				try { Thread.sleep(100); } catch (InterruptedException e_) {}
			}
		}

		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			out.write(string);
			out.flush();
			while (s.isConnected()) {
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				if(in.ready()) logger.println(in.readLine());
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private String getHost() {
		return "127.0.0.1";
	}

	private int getPort() {
		return 4243;
	}

	/**
	 * This method will return the command intercepter as per the node OS
	 * 
	 * @param launcher
	 * @param script
	 * @return CommandInterpreter
	 */
	private CommandInterpreter getCommandInterpreter(Launcher launcher, String script) {
		if (launcher.isUnix())
			return new Shell(script);
		else
			return new BatchFile(script);
	}
}
