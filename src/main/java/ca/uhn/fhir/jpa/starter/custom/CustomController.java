package ca.uhn.fhir.jpa.starter.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomController {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private LoincService loincService;
	public CustomController(LoincService loincService) {
		this.loincService = loincService;
	}
	@GetMapping("/test")
	public String test() {
		logger.info("Test endpoint called");
		loincService.testDB();
		return "Hello";
	}
}

