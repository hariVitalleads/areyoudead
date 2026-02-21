package com.checkin.repository;
import com.checkin.model.Registration;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {}

