package ca.uhn.fhir.jpa.starter.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoincService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private LoincRepository loincRepository;
	public LoincService(LoincRepository loincRepository) {
		this.loincRepository = loincRepository;
	}

	public String testDB() {
		if (loincRepository.findByCode("15074-9").isPresent()) {
			logger.info("Loinc already exists");
			return "Loinc already exists";
		}
		logger.info("invalid loinc code");
		return "Invalid Loinc code";
	};

	public void validate(String code) {
		if (loincRepository.findByCode(code).isEmpty()) {
			logger.info("Loinc code not found");
			throw new LoincValidationException("Invalid Loinc Code " + code, code);
		}
		logger.info("Valid Loinc code");
	}
}
