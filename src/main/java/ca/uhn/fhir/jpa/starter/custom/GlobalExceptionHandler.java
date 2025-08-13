package ca.uhn.fhir.jpa.starter.custom;

import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueTypeEnum;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(LoincValidationException.class)
	public ResponseEntity<OperationOutcome> handleLoincError(LoincValidationException ex) {
		OperationOutcome outcome = new OperationOutcome();

		outcome.addIssue()
			.setSeverity(IssueSeverityEnum.ERROR)
			.setCode(IssueTypeEnum.INVALID_CODE)
			.setDiagnostics("Invalid LOINC code: " + ex.getInvalidCode())
			.addLocation("Observation.code.coding.code");

		return ResponseEntity
			.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(outcome);
	}
}