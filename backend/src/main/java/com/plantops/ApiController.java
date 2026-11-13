package com.plantops;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static com.plantops.Domain.*;

@RestController @RequestMapping("/api")
class ApiController {
 private final UserRepository users; private final AssetRepository assets; private final WorkOrderRepository orders; private final ScheduleRepository schedules; private final AlertRepository alerts; private final AuditRepository audit; private final ReadingRepository readings; private final PlantOpsService service; private final JwtTokens tokens; private final PasswordEncoder encoder;
 ApiController(UserRepository u,AssetRepository a,WorkOrderRepository o,ScheduleRepository s,AlertRepository al,AuditRepository au,ReadingRepository r,PlantOpsService ps,JwtTokens jt,PasswordEncoder pe){users=u;assets=a;orders=o;schedules=s;alerts=al;audit=au;readings=r;service=ps;tokens=jt;encoder=pe;}
 String actor(Authentication a){return a.getName();} AppUser current(Authentication a){return users.findByEmail(actor(a)).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 Asset asset(Long id){return assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found"));}
 WorkOrder order(Long id){return orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Work order not found"));}
 AppUser user(Long id){return users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));}
 record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
 record LoginResponse(String token,UserView user){}
 record UserView(Long id,String email,String name,Role role){static UserView of(AppUser u){return new UserView(u.id,u.email,u.name,u.role);}}
 record AssetInput(@NotBlank String assetTag,@NotBlank String name,@NotBlank String location,String serialNumber,String manufacturer,String model,String specifications,@NotBlank String status,Double uptimePercent,Double temperatureC,Double vibrationMmS){}
 record WorkInput(@NotBlank String title,String description,@NotNull Long assetId,Long assigneeId,Priority priority,LocalDate dueDate){}
 record StatusInput(@NotNull WorkStatus status){}
 record ScheduleInput(@NotBlank String title,@NotNull Long assetId,@Min(1) int intervalDays,@NotNull LocalDate nextDueDate,String instructions,Boolean active){}
 record ReadingInput(@NotNull Long assetId,@PositiveOrZero double temperatureC,@PositiveOrZero double vibrationMmS){}
 record UserInput(@Email @NotBlank String email,@NotBlank String name,@NotBlank String password,@NotNull Role role){}

 @GetMapping("/health") Map<String,String> health(){return Map.of("status","UP");}
 @PostMapping("/auth/login") ResponseEntity<?> login(@Valid @RequestBody LoginRequest body){AppUser u=users.findByEmail(body.email).orElse(null);if(u==null||!encoder.matches(body.password,u.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");return ResponseEntity.ok(new LoginResponse(tokens.issue(u),UserView.of(u)));}
 @GetMapping("/auth/me") UserView me(Authentication a){return UserView.of(current(a));}

 @GetMapping("/dashboard") Map<String,Object> dashboard(Authentication authentication){service.runDueSchedules();service.markOverdue();List<Asset> all=assets.findAll();List<WorkOrder> active=orders.findAll().stream().filter(w->w.status!=WorkStatus.COMPLETED&&w.status!=WorkStatus.CANCELLED).toList();AppUser me=current(authentication);List<WorkOrder> recent=orders.findAll().stream().filter(w->me.role!=Role.TECHNICIAN||(w.assignee!=null&&w.assignee.id.equals(me.id))).sorted(Comparator.comparing((WorkOrder w)->w.createdAt).reversed()).limit(5).toList();return Map.of("assetCount",all.size(),"operationalCount",all.stream().filter(x->"Operational".equals(x.status)).count(),"attentionCount",all.stream().filter(x->"Attention".equals(x.status)).count(),"openWorkOrders",active.size(),"overdueWorkOrders",active.stream().filter(w->w.dueDate!=null&&w.dueDate.isBefore(LocalDate.now())).count(),"unreadAlerts",alerts.countByReadFalse(),"uptime",all.isEmpty()?0:Math.round(all.stream().mapToDouble(x->x.uptimePercent).average().orElse(0)*10.0)/10.0,"recentOrders",recent,"assets",all);}

 @GetMapping("/assets") List<Asset> listAssets(){return assets.findAll();}
 @PostMapping("/assets") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") Asset createAsset(@Valid @RequestBody AssetInput x,Authentication a){Asset q=new Asset(x.assetTag,x.name,x.location,x.serialNumber,x.manufacturer,x.model,x.specifications,x.status,x.uptimePercent==null?99:x.uptimePercent,x.temperatureC==null?25:x.temperatureC,x.vibrationMmS==null?1:x.vibrationMmS);q=assets.save(q);service.log(actor(a),"CREATED","Asset",q.id,q.name);return q;}
 @PutMapping("/assets/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") Asset updateAsset(@PathVariable Long id,@Valid @RequestBody AssetInput x,Authentication a){Asset q=asset(id);q.assetTag=x.assetTag;q.name=x.name;q.location=x.location;q.serialNumber=x.serialNumber;q.manufacturer=x.manufacturer;q.model=x.model;q.specifications=x.specifications;q.status=x.status;if(x.uptimePercent!=null)q.uptimePercent=x.uptimePercent;Asset saved=assets.save(q);service.log(actor(a),"UPDATED","Asset",id,saved.name);return saved;}
 @DeleteMapping("/assets/{id}") @PreAuthorize("hasRole('ADMIN')") void deleteAsset(@PathVariable Long id,Authentication a){Asset q=asset(id);String name=q.name;assets.delete(q);service.log(actor(a),"DELETED","Asset",id,name);}

 @GetMapping("/work-orders") List<WorkOrder> listOrders(Authentication a){AppUser me=current(a);return me.role==Role.TECHNICIAN?orders.findByAssigneeId(me.id):orders.findAll();}
 @PostMapping("/work-orders") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") WorkOrder createOrder(@Valid @RequestBody WorkInput x,Authentication a){WorkOrder w=new WorkOrder(x.title,x.description,asset(x.assetId),x.assigneeId==null?null:user(x.assigneeId),x.assigneeId==null?WorkStatus.OPEN:WorkStatus.ASSIGNED,x.priority==null?Priority.MEDIUM:x.priority,x.dueDate);w=orders.save(w);service.log(actor(a),"CREATED","WorkOrder",w.id,w.title);return w;}
 @PatchMapping("/work-orders/{id}/status") WorkOrder updateStatus(@PathVariable Long id,@Valid @RequestBody StatusInput x,Authentication a){WorkOrder w=order(id);AppUser me=current(a);if(me.role==Role.TECHNICIAN&&(!Objects.equals(w.assignee==null?null:w.assignee.id,me.id)||!(x.status==WorkStatus.IN_PROGRESS||x.status==WorkStatus.COMPLETED)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Technicians may update status only on assigned work");if(x.status==WorkStatus.ASSIGNED&&w.assignee==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assign a technician before setting status to Assigned");w.status=x.status;if(x.status==WorkStatus.COMPLETED)w.completedAt=LocalDateTime.now();w=orders.save(w);service.log(actor(a),"STATUS_CHANGED","WorkOrder",w.id,x.status.name());return w;}
 @PatchMapping("/work-orders/{id}/assign/{userId}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") WorkOrder assign(@PathVariable Long id,@PathVariable Long userId,Authentication a){WorkOrder w=order(id);AppUser u=user(userId);if(u.role!=Role.TECHNICIAN)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assignee must have the technician role");w.assignee=u;if(w.status==WorkStatus.OPEN)w.status=WorkStatus.ASSIGNED;w=orders.save(w);service.log(actor(a),"ASSIGNED","WorkOrder",w.id,u.name);return w;}

 @GetMapping("/schedules") List<MaintenanceSchedule> listSchedules(){return schedules.findAll();}
 @PostMapping("/schedules") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") MaintenanceSchedule createSchedule(@Valid @RequestBody ScheduleInput x,Authentication a){MaintenanceSchedule s=new MaintenanceSchedule(x.title,asset(x.assetId),x.intervalDays,x.nextDueDate,x.instructions);if(x.active!=null)s.active=x.active;s=schedules.save(s);service.log(actor(a),"CREATED","MaintenanceSchedule",s.id,s.title);return s;}
 @PutMapping("/schedules/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") MaintenanceSchedule updateSchedule(@PathVariable Long id,@Valid @RequestBody ScheduleInput x,Authentication a){MaintenanceSchedule s=schedules.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));s.title=x.title;s.asset=asset(x.assetId);s.intervalDays=x.intervalDays;s.nextDueDate=x.nextDueDate;s.instructions=x.instructions;if(x.active!=null)s.active=x.active;s=schedules.save(s);service.log(actor(a),"UPDATED","MaintenanceSchedule",s.id,s.title);return s;}
 @DeleteMapping("/schedules/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") void deleteSchedule(@PathVariable Long id,Authentication a){MaintenanceSchedule s=schedules.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));schedules.delete(s);service.log(actor(a),"DELETED","MaintenanceSchedule",id,s.title);}

 @GetMapping("/alerts") List<AlertRecord> listAlerts(){return alerts.findTop100ByOrderByCreatedAtDesc();}
 @PatchMapping("/alerts/{id}/read") AlertRecord markRead(@PathVariable Long id,Authentication a){AlertRecord x=alerts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));x.read=true;x=alerts.save(x);service.log(actor(a),"MARKED_READ","Alert",x.id,x.title);return x;}
 @PatchMapping("/alerts/read-all") Map<String,String> markAllRead(Authentication a){List<AlertRecord> unread=alerts.findAll().stream().filter(x->!x.read).peek(x->x.read=true).toList();alerts.saveAll(unread);service.log(actor(a),"MARKED_ALL_READ","Alert",null,"Marked alerts as read");return Map.of("message","All alerts marked as read");}

 @GetMapping("/readings") List<Reading> latestReadings(){return readings.findTop20ByOrderByRecordedAtDesc();}
 @GetMapping("/assets/{id}/readings") List<Reading> assetReadings(@PathVariable Long id){return readings.findTop30ByAssetIdOrderByRecordedAtDesc(id);}
 @PostMapping("/readings") @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')") Reading recordReading(@Valid @RequestBody ReadingInput x,Authentication a){Asset asset=asset(x.assetId);Reading r=readings.save(new Reading(asset,x.temperatureC,x.vibrationMmS));asset.temperatureC=x.temperatureC;asset.vibrationMmS=x.vibrationMmS;boolean abnormal=x.temperatureC>=80||x.vibrationMmS>=4.5;asset.status=abnormal?"Attention":"Operational";assets.save(asset);if(abnormal&&!alerts.existsByAssetIdAndTypeAndReadFalse(asset.id,AlertType.ABNORMAL_READING))alerts.save(new AlertRecord(AlertType.ABNORMAL_READING,"Abnormal reading detected",asset.name+" reported "+x.temperatureC+"°C and "+x.vibrationMmS+" mm/s.",asset));service.log(actor(a),"READING_RECORDED","Asset",asset.id,"Temperature "+x.temperatureC+"°C, vibration "+x.vibrationMmS+" mm/s");return r;}

 @GetMapping("/audit") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") List<AuditEntry> auditLog(){return audit.findTop100ByOrderByCreatedAtDesc();}
 @GetMapping("/users") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')") List<UserView> listUsers(){return users.findAll().stream().map(UserView::of).toList();}
 @PostMapping("/users") @PreAuthorize("hasRole('ADMIN')") UserView createUser(@Valid @RequestBody UserInput x,Authentication a){if(users.findByEmail(x.email).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");AppUser u=users.save(new AppUser(x.email,x.name,encoder.encode(x.password),x.role));service.log(actor(a),"CREATED","User",u.id,u.email);return UserView.of(u);}
}
