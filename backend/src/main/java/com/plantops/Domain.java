package com.plantops;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.*;

public final class Domain {
    private Domain() {}
    public enum Role { ADMIN, MANAGER, TECHNICIAN }
    public enum WorkStatus { OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum AlertType { ABNORMAL_READING, MAINTENANCE_OVERDUE, WORK_ORDER }

    @Entity @Table(name="app_users") @Getter @Setter @NoArgsConstructor
    public static class AppUser {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true) public String email;
        @Column(nullable=false) public String name;
        @JsonIgnore @Column(nullable=false) public String passwordHash;
        @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role;
        public AppUser(String email,String name,String passwordHash,Role role){this.email=email;this.name=name;this.passwordHash=passwordHash;this.role=role;}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class Asset {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true) public String assetTag;
        @Column(nullable=false) public String name;
        @Column(nullable=false) public String location;
        public String serialNumber,manufacturer,model,specifications;
        @Column(nullable=false) public String status="Operational";
        public double uptimePercent=98.5;
        public LocalDate commissionedDate;
        public double temperatureC=62.0, vibrationMmS=2.0;
        public Asset(String assetTag,String name,String location,String serial,String manufacturer,String model,String specs,String status,double uptime,double temp,double vibration){this.assetTag=assetTag;this.name=name;this.location=location;this.serialNumber=serial;this.manufacturer=manufacturer;this.model=model;this.specifications=specs;this.status=status;this.uptimePercent=uptime;this.temperatureC=temp;this.vibrationMmS=vibration;this.commissionedDate=LocalDate.now().minusYears(2);}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class WorkOrder {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public String title;
        @Column(length=2000) public String description;
        @ManyToOne(optional=false) public Asset asset;
        @ManyToOne public AppUser assignee;
        @Enumerated(EnumType.STRING) @Column(nullable=false) public WorkStatus status=WorkStatus.OPEN;
        @Enumerated(EnumType.STRING) @Column(nullable=false) public Priority priority=Priority.MEDIUM;
        public LocalDate dueDate;
        public LocalDateTime createdAt=LocalDateTime.now();
        public LocalDateTime completedAt;
        public WorkOrder(String title,String description,Asset asset,AppUser assignee,WorkStatus status,Priority priority,LocalDate due){this.title=title;this.description=description;this.asset=asset;this.assignee=assignee;this.status=status;this.priority=priority;this.dueDate=due;}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class MaintenanceSchedule {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public String title;
        @ManyToOne(optional=false) public Asset asset;
        @Column(nullable=false) public int intervalDays;
        @Column(nullable=false) public LocalDate nextDueDate;
        public String instructions;
        public boolean active=true;
        public MaintenanceSchedule(String title,Asset asset,int intervalDays,LocalDate nextDueDate,String instructions){this.title=title;this.asset=asset;this.intervalDays=intervalDays;this.nextDueDate=nextDueDate;this.instructions=instructions;}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class AlertRecord {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Enumerated(EnumType.STRING) @Column(nullable=false) public AlertType type;
        @Column(nullable=false) public String title;
        @Column(length=1000) public String message;
        @ManyToOne public Asset asset;
        public boolean read=false;
        public LocalDateTime createdAt=LocalDateTime.now();
        public AlertRecord(AlertType type,String title,String message,Asset asset){this.type=type;this.title=title;this.message=message;this.asset=asset;}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class AuditEntry {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public String actor;
        @Column(nullable=false) public String action;
        @Column(nullable=false) public String entityType;
        public Long entityId;
        @Column(length=1000) public String details;
        public LocalDateTime createdAt=LocalDateTime.now();
        public AuditEntry(String actor,String action,String entityType,Long entityId,String details){this.actor=actor;this.action=action;this.entityType=entityType;this.entityId=entityId;this.details=details;}
    }
    @Entity @Getter @Setter @NoArgsConstructor
    public static class Reading {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @ManyToOne(optional=false) public Asset asset;
        public double temperatureC,vibrationMmS;
        @Column(nullable=false) public LocalDateTime recordedAt=LocalDateTime.now();
        public Reading(Asset asset,double temperature,double vibration){this.asset=asset;this.temperatureC=temperature;this.vibrationMmS=vibration;}
    }
}
