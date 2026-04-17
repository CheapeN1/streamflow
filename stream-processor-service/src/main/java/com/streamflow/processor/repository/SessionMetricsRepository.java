package com.streamflow.processor.repository;

import com.streamflow.processor.model.entity.SessionMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionMetricsRepository extends JpaRepository<SessionMetricsEntity, String> {}
