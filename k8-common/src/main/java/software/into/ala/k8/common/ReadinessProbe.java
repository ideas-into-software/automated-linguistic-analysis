package software.into.ala.k8.common;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.CheckStatus.State;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyCheck;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

@SuppressWarnings("serial")
@Component
@HttpWhiteboardServletPattern("/probe/readiness")
public class ReadinessProbe extends HttpServlet implements SystemReadyCheck, Servlet {
	private static final String NAME = "Test Readiness Probe";
	private CheckStatus.State state = State.GREEN;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public CheckStatus getStatus() {
		return new CheckStatus(getName(), StateType.READY, state, "This is a test readiness probe");
	}
}