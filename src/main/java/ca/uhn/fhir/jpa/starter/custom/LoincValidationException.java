package ca.uhn.fhir.jpa.starter.custom;

public class LoincValidationException extends RuntimeException {
	private final String invalidCode;

	public LoincValidationException(String message, String invalidCode) {
		super(message);
		this.invalidCode = invalidCode;
	}

	public String getInvalidCode() {
		return invalidCode;
	}
}
