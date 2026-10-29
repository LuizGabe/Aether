package com.aether.api.repository;

import com.aether.api.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, String> {
}
