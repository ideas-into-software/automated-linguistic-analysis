package software.into.ala.service.messaging.impl.internal;

import org.apache.camel.builder.RouteBuilder;

public class SimpleRoute extends RouteBuilder {
	private String fromUri;
	private String toUri;

	public SimpleRoute(String fromUri, String toUri) {
		this.fromUri = fromUri;
		this.toUri = toUri;
	}

	@Override
	public void configure() throws Exception {
		from(this.fromUri).to(this.toUri);
	}
}