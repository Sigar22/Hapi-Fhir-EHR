package ca.uhn.fhir.jpa.starter.custom;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoincService {
	private final LoincRepository loincRepository;

	public LoincService(LoincRepository loincRepository) {
		this.loincRepository = loincRepository;
	}

	public void validate(String code) throws LoincValidationException {
		if (code == null || code.isBlank()) {
			throw new LoincValidationException("LOINC code cannot be empty", code);
		}

		if (loincRepository.findByCode(code).isEmpty()) {
			throw new LoincValidationException("LOINC code not found: " + code,code);
		}
	}
}