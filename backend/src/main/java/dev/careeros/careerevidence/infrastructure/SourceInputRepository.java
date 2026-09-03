package dev.careeros.careerevidence.infrastructure;

import dev.careeros.careerevidence.domain.SourceInput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SourceInputRepository extends JpaRepository<SourceInput, UUID> {
}
