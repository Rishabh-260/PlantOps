package com.plantops;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.concurrent.ThreadLocalRandom;
import static com.plantops.Domain.*;

@Service
class PlantOpsService {
 private final AssetRepository assets; private final WorkOrderRepository orders; private final ScheduleRepository schedules; private final AlertRepository alerts; private final AuditRepository audit; private final ReadingRepository readings;
 PlantOpsService(AssetRepository a,WorkOrderRepository o,ScheduleRepository s,AlertRepository al,AuditRepository au,ReadingRepository r){assets=a;orders=o;schedules=s;alerts=al;audit=au;readings=r;}
 void log(String actor,String action,String type,Long id,String details){audit.save(new AuditEntry(actor,action,type,id,details));}
 @Scheduled(cron="0 5 0 * * *") @Transactional void runDueSchedules(){LocalDate today=LocalDate.now();for(var schedule:schedules.findByActiveTrueAndNextDueDateLessThanEqual(today)){
   if(!orders.existsByAssetIdAndTitleAndDueDateAndStatusNot(schedule.asset.id,schedule.title,today,WorkStatus.CANCELLED)) orders.save(new WorkOrder(schedule.title,schedule.instructions,schedule.asset,null,WorkStatus.OPEN,Priority.MEDIUM,today));
   if(schedule.nextDueDate==null)schedule.nextDueDate=today.plusDays(schedule.intervalDays);else while(!schedule.nextDueDate.isAfter(today))schedule.nextDueDate=schedule.nextDueDate.plusDays(schedule.intervalDays);
   schedules.save(schedule);
   if(!alerts.existsByAssetIdAndTypeAndReadFalse(schedule.asset.id,AlertType.MAINTENANCE_OVERDUE)) alerts.save(new AlertRecord(AlertType.MAINTENANCE_OVERDUE,"Preventive maintenance due",schedule.title+" is due for "+schedule.asset.name+".",schedule.asset));
   log("system","GENERATED","MaintenanceSchedule",schedule.id,"Created work order from recurring schedule");
  }}
 @Scheduled(fixedDelay=20000) @Transactional void simulateReadings(){if(assets.count()==0)return;for(Asset asset:assets.findAll()){
   double temp=asset.temperatureC+(ThreadLocalRandom.current().nextDouble(-1.8,1.8)); double vib=asset.vibrationMmS+ThreadLocalRandom.current().nextDouble(-.35,.35);
   if(asset.assetTag.equals("C-0315")){temp=77+ThreadLocalRandom.current().nextDouble(0,4);vib=4.25+ThreadLocalRandom.current().nextDouble(0,.65);}
   asset.temperatureC=Math.round(temp*10.0)/10.0;asset.vibrationMmS=Math.round(vib*10.0)/10.0;
   boolean abnormal=asset.temperatureC>=80||asset.vibrationMmS>=4.5;
   asset.status=abnormal?"Attention":"Operational";assets.save(asset);readings.save(new Reading(asset,asset.temperatureC,asset.vibrationMmS));
   if(abnormal&&!alerts.existsByAssetIdAndTypeAndReadFalse(asset.id,AlertType.ABNORMAL_READING)){
      String metric=asset.temperatureC>=80?"temperature":"vibration";double value=asset.temperatureC>=80?asset.temperatureC:asset.vibrationMmS;String unit=asset.temperatureC>=80?"°C":" mm/s";
      alerts.save(new AlertRecord(AlertType.ABNORMAL_READING,"Abnormal reading detected",asset.name+" reported "+value+unit+" "+metric+".",asset));log("system","ALERT_CREATED","Asset",asset.id,"Threshold exceeded: "+metric);
   }
  }}
 @Scheduled(cron="0 10 0 * * *") @Transactional void markOverdue(){for(WorkOrder w:orders.findAll())if(w.dueDate!=null&&w.dueDate.isBefore(LocalDate.now())&&(w.status==WorkStatus.OPEN||w.status==WorkStatus.ASSIGNED||w.status==WorkStatus.IN_PROGRESS)&&!alerts.existsByAssetIdAndTypeAndReadFalse(w.asset.id,AlertType.MAINTENANCE_OVERDUE))alerts.save(new AlertRecord(AlertType.MAINTENANCE_OVERDUE,"Work order overdue",w.title+" for "+w.asset.name+" is overdue.",w.asset));}
}
