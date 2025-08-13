package ca.uhn.fhir.jpa.starter.custom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoincRepository extends JpaRepository<LoincCode,String> {
	List<LoincCode> findByUnits(String units);
	Optional<LoincCode> findByCode(String code);
}
