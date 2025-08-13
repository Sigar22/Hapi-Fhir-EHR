package ca.uhn.fhir.jpa.starter.custom;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.Coding;
import org.hl7.fhir.Observation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Interceptor
@Component
public class CustomFhirHook {
	private final Logger ourLog = LoggerFactory.getLogger(CustomFhirHook.class);
	private LoincService loincService;
	public CustomFhirHook(LoincService loincService) {
		this.loincService = loincService;
	}
	@PostConstruct
	public void init() {
		ourLog.info("CustomFhirHook initialized!");
	}

	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void handleResourceCreate(IBaseResource theResource, RequestDetails theRequestDetails) {
		if (theResource instanceof Patient) {
			Patient patient = (Patient) theResource;
			ourLog.info("CustomFhirHook handleResourceCreate patient: {}", patient);
		}
	}



	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void validateObservationCode(RequestDetails request, IBaseResource resource) {
		if (resource == null || !resource.getClass().getSimpleName().equals("Observation")) {
			return;
		}

		try {
			// Получаем код через рефлексию (универсально для DSTU2/DSTU3/R4)
			Object code = resource.getClass().getMethod("getCode").invoke(resource);
			if (code == null) {
				ourLog.warn("Observation has no code!");
				return;
			}

			// Получаем список coding (метод отличается в разных версиях)
			Object codingList = code.getClass().getMethod("getCoding").invoke(code);
			if (codingList instanceof List<?> && !((List<?>) codingList).isEmpty()) {
				Object firstCoding = ((List<?>) codingList).get(0);
				String system = (String) firstCoding.getClass().getMethod("getSystem").invoke(firstCoding);
				String codeValue = (String) firstCoding.getClass().getMethod("getCode").invoke(firstCoding);
				try {
					loincService.validate(codeValue);
				} catch (LoincValidationException e) {
					// Преобразуем в OperationOutcome для FHIR
					throw new UnprocessableEntityException(
						"LOINC validation failed: " + e.getMessage()
					);
				}
				ourLog.info("Found LOINC code: {} | System: {}", codeValue, system);
			} else {
				ourLog.warn("No coding entries found in Observation");
			}
		} catch (Exception e) {
			ourLog.error("Failed to extract code from Observation", e);
		}
	}

}
