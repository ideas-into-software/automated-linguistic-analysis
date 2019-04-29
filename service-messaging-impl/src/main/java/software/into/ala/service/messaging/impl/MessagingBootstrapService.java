package software.into.ala.service.messaging.impl;

import java.util.Collections;
import java.util.List;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.scr.AbstractCamelRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, property = { "camelContextId=alaCamelContext", "active=true" })
public class MessagingBootstrapService extends AbstractCamelRunner {
	private static final Logger LOG = LoggerFactory.getLogger(MessagingBootstrapService.class);

	@Override
	protected void setupCamelContext(BundleContext bundleContext, String camelContextId) throws Exception {
		LOG.debug("Setting up Camel Context ID: " + camelContextId);

		super.setupCamelContext(bundleContext, camelContextId);

		// Use MDC logging
		getContext().setUseMDCLogging(true);

		// Use breadcrumb logging
		getContext().setUseBreadcrumb(true);

		// Auto-startup
		getContext().setAutoStartup(true);
	}

	@Override
	protected List<RoutesBuilder> getRouteBuilders() throws Exception {
		return Collections.emptyList();
	}
}
