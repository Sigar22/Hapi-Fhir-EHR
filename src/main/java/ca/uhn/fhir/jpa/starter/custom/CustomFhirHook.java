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
			// 1. Проверка LOINC кода
			Optional<String> loincCode = getLoincCode(theResource);
			if (loincCode.isPresent()) {
				loincService.validate(loincCode.get());
			} else {
				ourLog.warn("Observation has no LOINC code");
				throwObservationError("Observation must contain a LOINC code.");
			}

			// 2. Проверка статуса "preliminary"
			Optional<String> status = getObservationStatus(theResource);
			if (!status.isPresent() || !"preliminary".equalsIgnoreCase(status.get())) {
				throwObservationError("Observation must have 'preliminary' status upon creation.");
			}

		} catch (LoincValidationException e) {
			throwObservationError("Invalid LOINC code: " + e.getInvalidCode());
		} catch (UnprocessableEntityException e) {
			// Перебрасываем уже сформированные исключения UnprocessableEntityException
			throw e;
		} catch (Exception e) {
			ourLog.error("Failed to validate Observation", e);
			throwObservationError("Internal server error during validation");
		}
	}

	private Optional<String> getLoincCode(IBaseResource observation) {
		try {
			// Get 'code' element (CodeableConcept)
			Object code = observation.getClass().getMethod("getCode").invoke(observation);
			if (code == null) return Optional.empty();

			// Get 'coding' list from CodeableConcept
			Object codingList = code.getClass().getMethod("getCoding").invoke(code);
			if (!(codingList instanceof java.util.List)) return Optional.empty();

			for (Object coding : (java.util.List<?>) codingList) {
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

	Optional<String> getObservationStatus(IBaseResource observation) {
		try {
			// Get 'status' element (CodeType or Enum)
			Object status = observation.getClass().getMethod("getStatus").invoke(observation);
			if (status == null) return Optional.empty();

			// If it's a CodeType (e.g., HAPI FHIR R4), getValue() returns the string
			// If it's an Enum (e.g., org.hl7.fhir.r4.model.Observation.ObservationStatus), toString() or name() might work
			// The most robust way is to check if it has a 'getValue' method, or just use toString()
			try {
				return Optional.ofNullable((String) status.getClass().getMethod("getValue").invoke(status));
			} catch (NoSuchMethodException e) {
				// If no getValue(), try toString() (e.g., for enums)
				return Optional.ofNullable(status.toString());
			}

		} catch (Exception e) {
			ourLog.error("Failed to extract status from Observation", e);
		}
		return Optional.empty();
	}

	private void throwObservationError(String message) {
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue()
			.setSeverity(IssueSeverity.ERROR)
			.setCode(IssueType.CODEINVALID) // Можно использовать более подходящий код, например, VALUE или REQUIRED
			.setDiagnostics(message);

		throw new UnprocessableEntityException(message, outcome);
	}
}