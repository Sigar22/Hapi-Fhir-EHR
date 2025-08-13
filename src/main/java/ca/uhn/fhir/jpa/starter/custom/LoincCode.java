package ca.uhn.fhir.jpa.starter.custom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loinc") // <-- ИЗМЕНИТЬ НА НИЖНИЙ РЕГИСТР
public class LoincCode {

	@Id
	@Column(name = "code") // Обычно имена колонок тоже в нижнем регистре. Если есть проблемы, поменяйте и их
	private String code;

	@Column(name = "units")
	private String units;

	public LoincCode() {}
	public String getLoincCode() { return code; }
	public void setLoincCode(String loincCode) { this.code = loincCode; }
	public String getUnits() { return units; }
	public void setUnits(String units) { this.units = units; }
}