package com.gnimah.backend.repository;

import com.gnimah.backend.entity.ActionVersement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionVersementRepository extends JpaRepository<ActionVersement, Long> {

    List<ActionVersement> findByVersementIdOrderByDateActionAsc(Long versementId);
}
