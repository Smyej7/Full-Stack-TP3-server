package fr.fullstack.shopapp.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
@Entity
public class SyncTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean syncCompleted;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public boolean isSyncCompleted() {
        return syncCompleted;
    }
    public void setSyncCompleted(boolean syncCompleted) {
        this.syncCompleted = syncCompleted;
    }
}