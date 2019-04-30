package org.smartregister.sample;

import com.evernote.android.job.JobManager;

import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.job.IndicatorGeneratorJobCreator;
import org.smartregister.repository.Repository;
import org.smartregister.sample.repository.SampleRepository;
import org.smartregister.view.activity.DrishtiApplication;


import static org.smartregister.util.Log.logError;

public class SampleApplication extends DrishtiApplication {

    private String indicatorsConfigFile = "config/indicator-definitions.yml";
    private String indicatorDataInitialisedPref = "INDICATOR_DATA_INITIALISED";
    private String appVersionCode = "APP_VERSION_CODE";

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;
        context = Context.getInstance();
        context.updateApplicationContext(getApplicationContext());
        CoreLibrary.init(context);
        repository = getRepository();
        ReportingLibrary.init(Context.getInstance(), repository, null, BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);
        ReportingLibrary reportingLibraryInstance = ReportingLibrary.getInstance();
        // Check if indicator data initialised
        boolean indicatorDataInitialised = Boolean.parseBoolean(reportingLibraryInstance.getContext()
                .allSharedPreferences().getPreference(indicatorDataInitialisedPref));
        String savedAppVersion = reportingLibraryInstance.getContext().allSharedPreferences().getPreference(appVersionCode);
        boolean isUpdated = checkIfAppUpdated(savedAppVersion);
        if (!indicatorDataInitialised || isUpdated) {
            reportingLibraryInstance.initIndicatorData(indicatorsConfigFile); // This will persist the data in the DB
            SampleRepository.addSampleData();
            reportingLibraryInstance.getContext().allSharedPreferences().savePreference(indicatorDataInitialisedPref, "true");
            reportingLibraryInstance.getContext().allSharedPreferences().savePreference(savedAppVersion, String.valueOf(BuildConfig.VERSION_CODE));
        }

        JobManager.create(this).addJobCreator(new IndicatorGeneratorJobCreator());
    }

    public Repository getRepository() {
        try {
            if (repository == null) {
                repository = new SampleRepository(getInstance().getApplicationContext(), context);
            }
        } catch (UnsatisfiedLinkError e) {
            logError("Error on getRepository: " + e);

        }
        return repository;
    }

    public static synchronized SampleApplication getInstance() {
        return (SampleApplication) mInstance;
    }

    private boolean checkIfAppUpdated(String savedAppVersion) {
        if (savedAppVersion.isEmpty()) {
            return true;
        } else {
            int savedVersion = Integer.parseInt(savedAppVersion);
            return (BuildConfig.VERSION_CODE > savedVersion);
        }
    }

    @Override
    public void logoutCurrentUser() {

    }
}
