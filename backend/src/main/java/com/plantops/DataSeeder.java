package com.plantops;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import static com.plantops.Domain.*;

@Configuration
class DataSeeder {
 @Bean CommandLineRunner seed(UserRepository users,AssetRepository assets,WorkOrderRepository orders,ScheduleRepository schedules,AlertRepository alerts,ReadingRepository readings,PasswordEncoder encoder){return args->{
   AppUser admin=users.findByEmail("admin@plantops.local").orElseGet(()->users.save(new AppUser("admin@plantops.local","Alex Morgan",encoder.encode("PlantOps123!"),Role.ADMIN)));
   AppUser manager=users.findByEmail("manager@plantops.local").orElseGet(()->users.save(new AppUser("manager@plantops.local","Jordan Lee",encoder.encode("PlantOps123!"),Role.MANAGER)));
   AppUser tech=users.findByEmail("tech@plantops.local").orElseGet(()->users.save(new AppUser("tech@plantops.local","Sam Patel",encoder.encode("PlantOps123!"),Role.TECHNICIAN)));
   if(assets.count()==0){
     Asset pump=assets.save(new Asset("P-1042","Cooling water pump","Utilities · Bay 2","CW-P-1042","Grundfos","CR 64-2","45 kW · 2900 rpm","Operational",99.2,64.2,2.1));
     Asset cnc=assets.save(new Asset("M-2088","CNC milling center","Production · Line A","HA-CNC-2088","Haas","VF-4SS","22.4 kW · 12000 rpm","Operational",97.8,58.4,1.7));
     Asset compressor=assets.save(new Asset("C-0315","Air compressor","Utilities · Compressor room","AC-0315-22","Atlas Copco","GA 45","45 kW · 7.5 bar","Attention",94.1,79.8,4.8));
     Asset conveyor=assets.save(new Asset("A-0077","Assembly conveyor","Production · Line B","CV-0077","Siemens","G120 drive","11 kW · 18 m/min","Operational",99.7,52.1,1.2));
     orders.save(new WorkOrder("Inspect compressor vibration","Investigate elevated vibration and check coupling alignment.",compressor,tech,WorkStatus.IN_PROGRESS,Priority.HIGH,LocalDate.now().plusDays(1)));
     orders.save(new WorkOrder("Replace pump seal","Seal kit is ready in the maintenance store.",pump,manager,WorkStatus.ASSIGNED,Priority.MEDIUM,LocalDate.now().plusDays(3)));
     orders.save(new WorkOrder("Verify CNC coolant filter","Check differential pressure and replace filter if needed.",cnc,null,WorkStatus.OPEN,Priority.LOW,LocalDate.now().plusDays(5)));
     schedules.save(new MaintenanceSchedule("Compressor weekly inspection",compressor,7,LocalDate.now().minusDays(1),"Inspect belt, coupling, oil level, and vibration."));
     schedules.save(new MaintenanceSchedule("Pump lubrication",pump,30,LocalDate.now().plusDays(4),"Verify grease level and inspect seals."));
     schedules.save(new MaintenanceSchedule("CNC spindle inspection",cnc,90,LocalDate.now().plusDays(12),"Inspect spindle runout and lubrication."));
     readings.save(new Reading(pump,64.2,2.1));readings.save(new Reading(cnc,58.4,1.7));readings.save(new Reading(compressor,79.8,4.8));readings.save(new Reading(conveyor,52.1,1.2));
     alerts.save(new AlertRecord(AlertType.ABNORMAL_READING,"Vibration above normal","Air compressor C-0315 reported 4.8 mm/s vibration (limit 4.5).",compressor));
   }
 };}
}
