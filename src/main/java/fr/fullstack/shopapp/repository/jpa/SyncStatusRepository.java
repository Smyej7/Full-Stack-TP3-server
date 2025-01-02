package fr.fullstack.shopapp.repository.jpa;

import fr.fullstack.shopapp.model.SyncTracker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStatusRepository extends JpaRepository<SyncTracker, Long> {
    boolean existsBySyncCompletedTrue();
}