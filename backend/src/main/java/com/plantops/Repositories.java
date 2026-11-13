package com.plantops;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
import static com.plantops.Domain.*;

final class Repositories { private Repositories() {} }
interface UserRepository extends JpaRepository<AppUser,Long> { Optional<AppUser> findByEmail(String email); List<AppUser> findByRole(Role role); }
interface AssetRepository extends JpaRepository<Asset,Long> { Optional<Asset> findByAssetTag(String tag); }
interface WorkOrderRepository extends JpaRepository<WorkOrder,Long> { List<WorkOrder> findByAssigneeId(Long id); boolean existsByAssetIdAndTitleAndDueDateAndStatusNot(Long assetId,String title,LocalDate dueDate,WorkStatus status); }
interface ScheduleRepository extends JpaRepository<MaintenanceSchedule,Long> { List<MaintenanceSchedule> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date); }
interface AlertRepository extends JpaRepository<AlertRecord,Long> { List<AlertRecord> findTop100ByOrderByCreatedAtDesc(); long countByReadFalse(); boolean existsByAssetIdAndTypeAndReadFalse(Long assetId,AlertType type); }
interface AuditRepository extends JpaRepository<AuditEntry,Long> { List<AuditEntry> findTop100ByOrderByCreatedAtDesc(); }
interface ReadingRepository extends JpaRepository<Reading,Long> { List<Reading> findTop30ByAssetIdOrderByRecordedAtDesc(Long assetId); List<Reading> findTop20ByOrderByRecordedAtDesc(); }
