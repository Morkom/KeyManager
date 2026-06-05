//package dev.morkom.keymanager.service;
//
//import dev.morkom.keymanager.model.AuditEvent;
//import dev.morkom.keymanager.repository.AuditEventRepository;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.List;
//
//@Service
//public class AuditEventService {
//
//    private final AuditEventRepository auditEventRepository;
//
//    public AuditEventService(AuditEventRepository auditEventRepository) {
//        this.auditEventRepository = auditEventRepository;
//    }
//
//    public void logEvent(String action, String details, boolean success) {
//        AuditEvent event = new AuditEvent();
//        event.setTimestamp(Instant.now());
//        event.setUsername(getCurrentUsername());
//        event.setAction(action);
//        event.setDetails(details);
//        event.setSuccess(success);
//        auditEventRepository.save(event);
//    }
//
//    public List<AuditEvent> getAllEvents() {
//        return auditEventRepository.findAll();
//    }
//
//    private String getCurrentUsername() {
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        if (principal instanceof UserDetails) {
//            return ((UserDetails) principal).getUsername();
//        } else if (principal != null) {
//            return principal.toString();
//        }
//        return "anonymous";
//    }
//}
