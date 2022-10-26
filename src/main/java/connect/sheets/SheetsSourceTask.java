package connect.sheets;

import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SheetsSourceTask extends SourceTask {
    private Integer pollInterval;

    private Long lastPollTime;

    private String spreadSheetId;

    private String memberNames;

    private String sprintNames;

    private AuthorizationCredentials authorizationCredentials;
    private final Logger sheetLogger = Logger.getLogger(SheetsSourceTask.class.getName());
    @Override
    public String version() {
        return null;
    }

    @Override
    public void start(Map<String, String> properties) {
        sheetLogger.info("connect-sheets: start");


        initializeAuthorizationCredentials(properties);
        initializeSpreadsheet(properties);


        sheetLogger.info(properties.toString());

        lastPollTime = 0L;
        if(SheetsSourceConfig.SHEET_INTERVAL_SECONDS_CONFIG_DEFAULT == null) {
            pollInterval = 3600;
        } else{
            //pollInterval = SheetsSourceConfig.SHEET_INTERVAL_SECONDS_CONFIG_DEFAULT;
            pollInterval = 60;
        }



    }

    private void initializeAuthorizationCredentials(final Map<String, String> properties) {
        sheetLogger.info("connect-sheets: Initialize Authorization credentials");
        authorizationCredentials = AuthorizationCredentials.getInstance(
                properties.get(SheetsSourceConfig.SHEET_TYPE),
                properties.get(SheetsSourceConfig.SHEET_PROJECT_ID),
                properties.get(SheetsSourceConfig.SHEET_PRIVATE_KEY_ID),
                properties.get(SheetsSourceConfig.SHEET_PRIVATE_KEY),
                properties.get(SheetsSourceConfig.SHEET_CLIENT_EMAIL),
                properties.get(SheetsSourceConfig.SHEET_CLIENT_ID),
                properties.get(SheetsSourceConfig.SHEET_AUTH_URI),
                properties.get(SheetsSourceConfig.SHEET_TOKEN_URI),
                properties.get(SheetsSourceConfig.SHEET_AUTH_PROVIDER_URL),
                properties.get(SheetsSourceConfig.SHEET_CLIENT_CERTIFICATION_URL));
    }

    private void initializeSpreadsheet(final Map<String, String> properties) {
        sheetLogger.info("connect-sheets: Initialize Spreadsheet");
        memberNames = properties.get(SheetsSourceConfig.SHEET_MEMBER_NAMES);
        sprintNames = properties.get(SheetsSourceConfig.SHEET_SPRINT_NAMES);
        if(SheetsSourceConfig.SPREADSHEET_ID == null) {
            createSpreadsheet(properties);
        } else {
            sheetLogger.info("connect-sheets: Spreadsheet exists");
            spreadSheetId = properties.get(SheetsSourceConfig.SPREADSHEET_ID);
        }
    }

    private void createSpreadsheet(final Map <String, String> properties) {

    }


    private boolean lostConnection() {
        return System.currentTimeMillis() < (lastPollTime + (1000));
    }



    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List <SourceRecord> records = new ArrayList<>();
        sheetLogger.info("lastPollDeltaMillis:" + (System.currentTimeMillis() - lastPollTime) + " interval:" + pollInterval);
        if(lastPollTime != 0 && lostConnection()) {
            Thread.sleep(1000);
            return records;
        }

        lastPollTime = System.currentTimeMillis();

        //Reading table
        sheetLogger.info("Reading table from Google Sheets");
        try {
            ValueRange values = SheetsApi.getValues(spreadSheetId, "A1");
            sheetLogger.info(values.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sheetLogger.info("Finish");
        return new ArrayList<>();
    }

    @Override
    public void stop() {

    }
}
