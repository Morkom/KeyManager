//package dev.morkom.keymanager.controller;
//
//import dev.morkom.keymanager.model.AuditEvent;
//import dev.morkom.keymanager.service.AuditEventService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/audit")
//public class AuditController {
//
//    private final AuditEventService auditEventService;
//
//    public AuditController(AuditEventService auditEventService) {
//        this.auditEventService = auditEventService;
//    }
//
//    @GetMapping
//    public ResponseEntity<List<AuditEvent>> getAuditEvents() {
//        return ResponseEntity.ok(auditEventService.getAllEvents());
//    }
//}
