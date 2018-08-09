import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.IOException;

public class WebScraperJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        WebScraper scraper = new WebScraper();
        WebDriver driver = null;
        String directory = System.getProperty("user.dir");
        try {
            String geckoDriver = scraper.loadConfig(directory + "/" + "config.txt");
            System.setProperty("webdriver.gecko.driver", geckoDriver);
            driver = new FirefoxDriver(); // captcha problem, need to put comments in developer box
            scraper.mainLogic(driver,scraper);
        }catch (Exception e){
            System.out.println("Using HtmlUnitDriver, system not configured for FirefoxDriver");
            driver = new HtmlUnitDriver(); // captcha problem, need to put comments in developer box
            try {
                scraper.mainLogic(driver,scraper);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

    }
}
