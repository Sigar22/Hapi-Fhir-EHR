package ca.uhn.fhir.jpa.starter.custom;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/observations") // Базовый путь для вашего не-FHIR API
public class ObservationManagementController {

	private static final Logger ourLog = LoggerFactory.getLogger(ObservationManagementController.class);

	@Autowired
	private IFhirResourceDao<Observation> myObservationDao;

	@Autowired
	private CustomFhirHook customFhirHook; // Для переиспользования логики получения статуса

	/**
	 * Эндпоинт для изменения статуса Observation на 'final'.
	 *
	 * Пример вызова:
	 * POST http://localhost:8080/api/observations/{id}/finalize
	 * Content-Type: application/json
	 * {
	 *   "comment": "Окончательная верификация проведена."
	 * }
	 *
	 * @param id ID ресурса Observation.
	 * @param payload Map, содержащая необязательный комментарий.
	 * @return Обновленный ресурс Observation или OperationOutcome в случае ошибки.
	 */
	@PostMapping("/{id}/finalize")
	public ResponseEntity<?> finalizeObservation(
		@PathVariable String id,
		@RequestBody(required = false) Map<String, String> payload) {

		String comment = (payload != null) ? payload.get("comment") : null;
		ourLog.info("Received /api/observations/{}/finalize request with comment: {}", id, comment);

		Observation observationToUpdate;
		try {
			// HAPI FHIR DAO требует IdType
			IdType observationId = new IdType("Observation", id);
			observationToUpdate = myObservationDao.read(observationId); // RequestDetails не нужен для чтения в таком контексте, HAPI предоставит его сам
		} catch (ResourceNotFoundException e) {
			ourLog.error("Observation {} not found for finalization.", id);
			return createErrorResponse(
				"Observation with ID " + id + " not found.",
				IssueType.NOTFOUND,
				IssueSeverity.ERROR,
				HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			ourLog.error("Error reading Observation {}: {}", id, e.getMessage(), e);
			return createErrorResponse(
				"Internal server error when reading Observation: " + e.getMessage(),
				IssueType.EXCEPTION,
				IssueSeverity.FATAL,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}

		Optional<String> currentStatus = getObservationStatusFromHook(observationToUpdate);
		if (currentStatus.isPresent() && "final".equalsIgnoreCase(currentStatus.get())) {
			return createErrorResponse(
				"Observation with ID " + id + " is already in 'final' status.",
				IssueType.INVALID, // Изменено на INVALID
				IssueSeverity.ERROR,
				HttpStatus.BAD_REQUEST);
		}
		if (currentStatus.isPresent() && !"preliminary".equalsIgnoreCase(currentStatus.get())) {
			return createErrorResponse(
				"Observation with ID " + id + " cannot be finalized from status '" + currentStatus.get() + "'. It must be 'preliminary'.",
				IssueType.INVALID, // Изменено на INVALID
				IssueSeverity.ERROR,
				HttpStatus.BAD_REQUEST);
		}

		observationToUpdate.setStatus(Observation.ObservationStatus.FINAL);

		// Если хотите добавить комментарий как Extension или в Note
		// Например:
		// if (comment != null && !comment.isEmpty()) {
		//    observationToUpdate.addNote().setText(comment);
		// }


		try {
			// При вызове myObservationDao.update() ваши интерцепторы (CustomFhirHook)
			// будут автоматически вызваны и выполнят валидацию.
			myObservationDao.update(observationToUpdate); // RequestDetails не нужен, HAPI его сам создаст/предоставит
			ourLog.info("Observation {} successfully finalized.", id);
			return ResponseEntity.ok(observationToUpdate); // Возвращаем обновленный ресурс
		} catch (UnprocessableEntityException e) {
			// Это исключения, брошенные вашим хуком (CustomFhirHook)
			ourLog.error("Validation failed for Observation {}: {}", id, e.getMessage());
			// Возвращаем OperationOutcome, который уже содержится в исключении HAPI FHIR
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getOperationOutcome());
		} catch (Exception e) {
			ourLog.error("An unexpected error occurred while finalizing Observation {}: {}", id, e.getMessage(), e);
			return createErrorResponse(
				"Failed to finalize Observation due to an internal error: " + e.getMessage(),
				IssueType.EXCEPTION,
				IssueSeverity.FATAL,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Вспомогательный метод для получения статуса, используем метод из CustomFhirHook
	private Optional<String> getObservationStatusFromHook(IBaseResource observation) {
		try {
			return customFhirHook.getObservationStatus(observation);
		} catch (Exception e) {
			ourLog.error("Error retrieving status via CustomFhirHook: {}", e.getMessage());
			return Optional.empty();
		}
	}

	// Вспомогательный метод для создания ResponseEntity с OperationOutcome
	private ResponseEntity<OperationOutcome> createErrorResponse(
		String message, IssueType issueType, IssueSeverity issueSeverity, HttpStatus httpStatus) {
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue()
			.setSeverity(issueSeverity)
			.setCode(issueType)
			.setDiagnostics(message);
		return ResponseEntity.status(httpStatus).body(outcome);
	}
}