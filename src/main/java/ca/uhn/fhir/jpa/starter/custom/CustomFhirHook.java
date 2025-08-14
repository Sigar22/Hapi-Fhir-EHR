package ca.uhn.fhir.jpa.starter.custom;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Interceptor
@Component
public class CustomFhirHook {
	private final Logger ourLog = LoggerFactory.getLogger(CustomFhirHook.class);
	private final LoincService loincService;

	public CustomFhirHook(LoincService loincService) {
		this.loincService = loincService;
	}

	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void validateObservation(IBaseResource theResource, RequestDetails theRequestDetails) {
		if (!"Observation".equals(theResource.fhirType())) {
			return;
		}

		try {
			Optional<String> loincCode = getLoincCode(theResource);
			if (loincCode.isPresent()) {
				loincService.validate(loincCode.get());
			} else {
				ourLog.warn("Observation has no LOINC code");
			}
		} catch (LoincValidationException e) {
			throwObservationError("Invalid LOINC code: " + e.getInvalidCode());
		} catch (Exception e) {
			ourLog.error("Failed to validate Observation", e);
			throwObservationError("Internal server error during validation");
		}
	}

	private Optional<String> getLoincCode(IBaseResource observation) {
		// Используем рефлексию для работы с разными версиями FHIR
		try {
			Object code = observation.getClass().getMethod("getCode").invoke(observation);
			if (code == null) return Optional.empty();

			Object codingList = code.getClass().getMethod("getCoding").invoke(code);
			if (!(codingList instanceof List)) return Optional.empty();

			for (Object coding : (List<?>) codingList) {
				String system = (String) coding.getClass().getMethod("getSystem").invoke(coding);
				if ("http://loinc.org".equals(system)) {
					String codeValue = (String) coding.getClass().getMethod("getCode").invoke(coding);
					return Optional.ofNullable(codeValue);
				}
			}
		} catch (Exception e) {
			ourLog.error("Failed to extract LOINC code from Observation", e);
		}
		return Optional.empty();
	}

	private void throwObservationError(String message) {
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue()
			.setSeverity(IssueSeverity.ERROR)
			.setCode(IssueType.CODEINVALID)
			.setDiagnostics(message);

		throw new UnprocessableEntityException(message, outcome);
	}
}