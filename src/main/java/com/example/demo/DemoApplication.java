package com.example.demo;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
@Controller
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	Logger logger = LoggerFactory.getLogger("DemoApplication");

	@GetMapping("singleheader")
	public ResponseEntity<StreamingResponseBody> singleheader() throws IOException {
		StreamingResponseBody ret = out -> {
			out.write("singleheader".getBytes());
		};
		return ResponseEntity.ok(ret);
	}


	@RequestMapping("/doubleheader")
	public ResponseEntity<String> doubleheader(HttpServletResponse resp) throws IOException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Transfer-Encoding",
				"chunked");
		return new ResponseEntity("doubleheader", responseHeaders, HttpStatus.OK);
	}

	@RequestMapping("/idheader")
	public ResponseEntity<String> idheader(HttpServletResponse resp) throws IOException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Transfer-Encoding",
				"identity");
		return new ResponseEntity("identityheader", responseHeaders, HttpStatus.OK);
	}


	@Bean
	public TomcatServletWebServerFactory containerFactory() {
		return new TomcatServletWebServerFactory() {
			@Override
			protected void configureContext(Context context, ServletContextInitializer[] initializers) {
				super.configureContext(context, initializers);
				context.getPipeline().addValve(new HeaderWarningValve());
			}
		};
	}

	private static final String TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String CHUNKED = "chunked";
	class HeaderWarningValve extends ValveBase {
		@Override
		public void invoke(Request request, Response response) throws IOException, ServletException {

			List headerVals = new ArrayList(3);
			for (int i = 0; i < request.getCoyoteRequest().getMimeHeaders().size(); i++) {
				String name = request.getCoyoteRequest().getMimeHeaders().getName(i).getString();
				String val = request.getCoyoteRequest().getMimeHeaders().getValue(i).getString();
				if(TRANSFER_ENCODING.equals(name)) {
					headerVals.add(val);
				}
			}

			logHeaders("Request", headerVals.iterator());
			headerVals.clear();

			Valve next = getNext();
			if (next != null) {
				next.invoke(request, response);
			}


			for (int i = 0; i < response.getCoyoteResponse().getMimeHeaders().size(); i++) {
				String name = response.getCoyoteResponse().getMimeHeaders().getName(i).getString();
				String val = response.getCoyoteResponse().getMimeHeaders().getValue(i).getString();

				if (TRANSFER_ENCODING.equals(name)) {
					headerVals.add(val);
				}
			}
			if(headerVals.size() > 0){
				logHeaders("Response", headerVals.iterator());
			}


		}

		private void logHeaders(String reqResp, Iterator<String> headerVals) {
			if (headerVals == null || !headerVals.hasNext()) {
				return;
			}

			int trxEncCount = 0;
			for(Iterator<String> it = headerVals; it.hasNext();) {
				String hdr = it.next();
				trxEncCount++;
				if (trxEncCount > 1) {
					logger.error("In {}.  Encountered {} header {} times", reqResp, TRANSFER_ENCODING, trxEncCount);
				}
				if (!CHUNKED.equals(hdr)) {
					logger.error("In {}.  Found unacceptable header value: {}", reqResp, hdr);
				}
			}


		}

		@Override
		public boolean isAsyncSupported() {
			return true;
		}
	}



}
