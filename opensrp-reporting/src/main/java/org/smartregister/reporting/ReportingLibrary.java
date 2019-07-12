package org.smartregister.reporting;

import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.Context;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.reporting.domain.IndicatorQuery;
import org.smartregister.reporting.domain.IndicatorYamlConfigItem;
import org.smartregister.reporting.domain.IndicatorsYamlConfig;
import org.smartregister.reporting.domain.ReportIndicator;
import org.smartregister.reporting.repository.DailyIndicatorCountRepository;
import org.smartregister.reporting.repository.IndicatorQueryRepository;
import org.smartregister.reporting.repository.IndicatorRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ReportingLibrary {

    private static boolean appOnDebugMode;
    private static final String APP_VERSION_CODE = "APP_VERSION_CODE";
    private static final String INDICATOR_DATA_INITIALISED = "INDICATOR_DATA_INITIALISED";
    private static ReportingLibrary instance;
    private Repository repository;
    private DailyIndicatorCountRepository dailyIndicatorCountRepository;
    private IndicatorQueryRepository indicatorQueryRepository;
    private IndicatorRepository indicatorRepository;
    private EventClientRepository eventClientRepository;
    private Context context;
    private CommonFtsObject commonFtsObject;
    private int applicationVersion;
    private int databaseVersion;
    private Yaml yaml;
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    private ReportingLibrary(Context context, Repository repository, CommonFtsObject commonFtsObject, int applicationVersion, int databaseVersion) {
        this.repository = repository;
        this.context = context;
        this.commonFtsObject = commonFtsObject;
        this.applicationVersion = applicationVersion;
        this.databaseVersion = databaseVersion;
        initRepositories();
    }

    public static void init(Context context, Repository repository, CommonFtsObject commonFtsObject, int applicationVersion, int databaseVersion) {
        if (instance == null) {
            instance = new ReportingLibrary(context, repository, commonFtsObject, applicationVersion, databaseVersion);
        }
        appOnDebugMode = BuildConfig.DEBUG;
    }

    public static ReportingLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call " + ReportingLibrary.class.getName() + ".init() in the onCreate() method of your Application class");
        }
        return instance;
    }

    private void initRepositories() {
        this.dailyIndicatorCountRepository = new DailyIndicatorCountRepository(getRepository());
        this.indicatorQueryRepository = new IndicatorQueryRepository(getRepository());
        this.indicatorRepository = new IndicatorRepository(getRepository());
        this.eventClientRepository = new EventClientRepository(getRepository());
    }

    public Repository getRepository() {
        return repository;
    }

    public DailyIndicatorCountRepository dailyIndicatorCountRepository() {
        if (dailyIndicatorCountRepository == null) {
            dailyIndicatorCountRepository = new DailyIndicatorCountRepository(getRepository());
        }

        return dailyIndicatorCountRepository;
    }

    public IndicatorQueryRepository indicatorQueryRepository() {
        if (indicatorQueryRepository == null) {
            indicatorQueryRepository = new IndicatorQueryRepository(getRepository());
        }

        return indicatorQueryRepository;
    }

    public IndicatorRepository indicatorRepository() {
        if (indicatorRepository == null) {
            indicatorRepository = new IndicatorRepository(getRepository());
        }

        return indicatorRepository;
    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository(getRepository());
        }

        return eventClientRepository;
    }

    public Context getContext() {
        return context;
    }

    public CommonFtsObject getCommonFtsObject() {
        return commonFtsObject;
    }

    public int getApplicationVersion() {
        return applicationVersion;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /***
     * Method that initializes the indicator queries.
     * Indicator queries are only read once on release mode but when on debug queries are refreshed
     * with content from the config files
     * @param configFilePath path of file containing the indicator definitions
     * @param sqLiteDatabase database to write the content obtained from the file
     */
    public void initIndicatorData(String configFilePath, SQLiteDatabase sqLiteDatabase) {

        if (!appOnDebugMode && (!isAppUpdated() || hasInitializedIndicators())) {
            return;
        }

        if (sqLiteDatabase != null) {
            indicatorRepository.truncateTable(sqLiteDatabase);
            indicatorQueryRepository.truncateTable(sqLiteDatabase);
        } else {
            indicatorRepository.truncateTable();
            indicatorQueryRepository.truncateTable();
        }

        initYamlIndicatorConfig();
        Iterable<Object> indicatorsFromFile = null;
        try {
            indicatorsFromFile = loadIndicatorsFromFile(configFilePath);
        } catch (IOException ioe) {
            Log.e("SampleApplication", ioe.getMessage());
        }
        if (indicatorsFromFile != null) {
            IndicatorsYamlConfig indicatorsConfig;
            List<ReportIndicator> reportIndicators = new ArrayList<>();
            List<IndicatorQuery> indicatorQueries = new ArrayList<>();
            ReportIndicator indicator;
            IndicatorQuery indicatorQuery;

            for (Object indicatorObject : indicatorsFromFile) {
                indicatorsConfig = (IndicatorsYamlConfig) indicatorObject;
                for (IndicatorYamlConfigItem indicatorYamlConfigItem : indicatorsConfig.getIndicators()) {
                    indicator = new ReportIndicator(null, indicatorYamlConfigItem.getKey(), indicatorYamlConfigItem.getDescription(), null);
                    indicatorQuery = new IndicatorQuery(null, indicatorYamlConfigItem.getKey(), indicatorYamlConfigItem.getIndicatorQuery(), 0);
                    reportIndicators.add(indicator);
                    indicatorQueries.add(indicatorQuery);
                }
            }
            if (sqLiteDatabase != null) {
                saveIndicators(reportIndicators, sqLiteDatabase);
                saveIndicatorQueries(indicatorQueries, sqLiteDatabase);
            } else {
                saveIndicators(reportIndicators);
                saveIndicatorQueries(indicatorQueries);
            }

            context.allSharedPreferences().savePreference(INDICATOR_DATA_INITIALISED, "true");
            context.allSharedPreferences().savePreference(APP_VERSION_CODE, String.valueOf(BuildConfig.VERSION_CODE));
        }
    }

    private void initYamlIndicatorConfig() {
        Constructor constructor = new Constructor(IndicatorsYamlConfig.class);
        TypeDescription typeDescription = new TypeDescription(IndicatorsYamlConfig.class);
        typeDescription.addPropertyParameters(IndicatorYamlConfigItem.INDICATOR_PROPERTY, IndicatorYamlConfigItem.class);
        constructor.addTypeDescription(typeDescription);
        yaml = new Yaml(constructor);
    }

    private Iterable<Object> loadIndicatorsFromFile(String configFilePath) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(Context.getInstance().applicationContext().getAssets().open(configFilePath));
        return yaml.loadAll(inputStreamReader);
    }

    private void saveIndicators(List<ReportIndicator> indicators) {
        for (ReportIndicator indicator : indicators) {
            this.indicatorRepository().add(indicator);
        }
    }

    private void saveIndicators(List<ReportIndicator> indicators, SQLiteDatabase sqLiteDatabase) {
        for (ReportIndicator indicator : indicators) {
            this.indicatorRepository().add(indicator, sqLiteDatabase);
        }
    }

    private void saveIndicatorQueries(List<IndicatorQuery> indicatorQueries) {
        for (IndicatorQuery indicatorQuery : indicatorQueries) {
            this.indicatorQueryRepository().add(indicatorQuery);
        }
    }

    private void saveIndicatorQueries(List<IndicatorQuery> indicatorQueries, SQLiteDatabase sqLiteDatabase) {
        for (IndicatorQuery indicatorQuery : indicatorQueries) {
            this.indicatorQueryRepository().add(indicatorQuery, sqLiteDatabase);
        }
    }

    private boolean isAppUpdated() {
        String savedAppVersion = ReportingLibrary.getInstance().getContext().allSharedPreferences().getPreference(APP_VERSION_CODE);
        if (savedAppVersion.isEmpty()) {
            return true;
        } else {
            return (BuildConfig.VERSION_CODE > Integer.parseInt(savedAppVersion));
        }

    }

    public boolean hasInitializedIndicators() {
        return Boolean.parseBoolean(context.allSharedPreferences().getPreference(INDICATOR_DATA_INITIALISED));
    }
}
