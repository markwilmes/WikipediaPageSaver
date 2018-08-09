import org.apache.log4j.BasicConfigurator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class CronRun {
    public CronRun() throws InterruptedException, SchedulerException {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);
        ScraperTrigger();
    }

    public void ScraperTrigger() throws SchedulerException, InterruptedException {
        BasicConfigurator.configure();
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        scheduler.start();

        JobDetail job = newJob(WebScraperJob.class)
                .withIdentity("scraperJob","group1")
                .build();

        Trigger trigger = newTrigger()
                .withIdentity("scraperJob","group1")
                .startNow()
                .withSchedule(simpleSchedule()
                .withIntervalInMinutes(5)
                .repeatForever())
                .build();

        scheduler.scheduleJob(job,trigger);

        while(true){
            Thread.sleep(5000);
        }
    }

    public static void main(String[] args) throws SchedulerException, InterruptedException {
        CronRun run = new CronRun();
    }

}
