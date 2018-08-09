import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class WebScraper{
    // uses the htmlHeadDriver in selenium, and jsoup to scrape the website.
    // stores the scrapped website in a sqlite3 database
    // Also checks with the database to ensure that the website hasn't changed
    // If it has changed, uses the resetSite function in the website to reinsert the original site
    // Quartz runs the cron job that executes this once every 5 minutes

    public WebScraper(){}

    public String scrapeSite(String url){
        try {
            System.out.println("Scraping site");
            Document doc = (Document) Jsoup.connect(url).get();
            Element element = doc.getElementById("wpTextbox1");
            return element.ownText();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean queryDatabase(String query){ // todo check to see if data retrieved is same as in database.
        // todo if data in database is not the same, run reset with retrieved data from database.
        String site = null;
        Connection connection = null;
        Statement state = null;

        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:WikiPages.db");
            query = query.replace("'","`");

            state = connection.createStatement();
            String sql = "SELECT PAGE_TITLE FROM PAGES WHERE PAGE = '" + query + "';";
            System.out.println(sql);

            System.out.println("Executing comparison query"); // sanitize to get rid of 's
            ResultSet result = state.executeQuery(sql);
            System.out.println("Query completed");
            while (result.next()){
                System.out.println(result);
                if(result.toString() == null){
                    return false;
                }else{
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getData(String key){ // todo if data is not there, run a scrape, and save that scrape to the database
        String site = null;
        Connection connection = null;
        Statement state = null;

        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:WikiPages.db");
            System.out.println("Connection with database established");

            state = connection.createStatement();
            String sql = "SELECT page FROM PAGES WHERE PAGE_TITLE = '" + key + "';";
            System.out.println("Executing query for data"); // sanitize to get rid of 's

            ResultSet result = state.executeQuery(sql);
            if(result.next()){
                site = result.toString();
                System.out.println("site is: " + site.toString());
            }else{
                String scraped = scrapeSite("https://en.wikipedia.org/wcd /index.php?title=" + key + "&action=edit");
                scraped = scraped.replace("'","`");
                ResultSet ID  = state.executeQuery("SELECT MAX(ID) FROM PAGES;");
                if(ID.next()){
                    System.out.println(ID.toString());
                    int id = ID.getInt(ID.getRow()) + 1;
                    String insertSQL = "INSERT INTO PAGES VALUES(" + id  + ",'" + key + "','" + scraped + "');";
                    state.execute(insertSQL);
                }else{
                    String insertSQL = "INSERT INTO PAGES VALUES(" + 1  + ",'" + key + "','" + scraped + "');";
                    state.execute(insertSQL);
                }

                site = scraped;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(site);
        return site;
    }

    public void resetSite(String site,String url,WebDriver driver) throws InterruptedException {
        site = site.replace("`","'");
        driver.get(url);
        driver.get(url);


        WebElement textArea = driver.findElement(By.id("wpTextbox1"));
        WebElement summaryText = driver.findElement(By.id("wpSummary"));
        System.out.println(textArea.getSize());
        String selectAll = Keys.chord(Keys.CONTROL, "a");

        textArea.sendKeys(selectAll);
        Thread.sleep(1000);
        textArea.sendKeys(Keys.DELETE);
        Thread.sleep(1000);
        textArea.sendKeys(site);
        System.out.println(textArea.getSize());
        Thread.sleep(1000);
        summaryText.sendKeys("Revision to previous version");
        Thread.sleep(1000);

        WebElement publish = driver.findElement(By.id("wpSave"));
        publish.click();
    }

    public String loadConfig(String filename) throws IOException {
        StringBuilder output = new StringBuilder();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(filename);
            br = new BufferedReader(fr);

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                output.append(sCurrentLine);
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fr != null){
                fr.close();
            }
            if(br != null){
                br.close();
            }
        }

        return String.valueOf(output);
    }

    public void mainLogic(WebDriver driver,WebScraper scraper) throws IOException, InterruptedException {
        String siteToPerm = scraper.loadConfig("siteConfig.txt");

        String website = scraper.scrapeSite("https://en.wikipedia.org/w/index.php?title=" + siteToPerm + "&action=edit");
        /*
        try {
            scraper.resetSite("https://en.wikipedia.org/w/index.php?title=Wikipedia:Sandbox&action=edit",driver);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        if(!scraper.queryDatabase(website)){
            String site = scraper.getData(siteToPerm);
            scraper.resetSite(site,"https://en.wikipedia.org/w/index.php?title=" + siteToPerm + "&action=edit",driver);
        }
    }
}
