package org.smartregister.reporting.impl;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.smartregister.reporting.R;
import org.smartregister.reporting.domain.IndicatorTally;
import org.smartregister.reporting.domain.PieChartSlice;
import org.smartregister.reporting.impl.models.IndicatorModel;
import org.smartregister.reporting.impl.models.PieChartViewModel;
import org.smartregister.reporting.impl.views.IndicatorView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.smartregister.reporting.BaseUnitTest.getDateTime;

@RunWith(MockitoJUnitRunner.class)
public class ReportingUtilTest {

    @Mock
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPieSelectionValueReturnsYes() {
        PieChartSlice pieChartSlice = new PieChartSlice(12, ReportingUtil.YES_GREEN_SLICE_COLOR);
        Mockito.when(context.getString(R.string.yes_slice_label)).thenReturn("Yes");
        Assert.assertEquals("Yes", ReportingUtil.getPieSelectionValue(pieChartSlice, context));
    }

    @Test
    public void testGetPieSelectionValueReturnsNo() {
        PieChartSlice pieChartSlice = new PieChartSlice(35, ReportingUtil.NO_RED_SLICE_COLOR);
        Mockito.when(context.getString(R.string.yes_slice_label)).thenReturn("No");
        Assert.assertEquals("No", ReportingUtil.getPieSelectionValue(pieChartSlice, context));
    }

    @Test
    public void testGetTotalStaticCount() {
        Map<String, IndicatorTally> tally1 = new HashMap<>();
        tally1.put("indicator1", new IndicatorTally(null, 3, "indicator1", null));
        Map<String, IndicatorTally> tally2 = new HashMap<>();
        tally2.put("indicator1", new IndicatorTally(null, 9, "indicator1", null));
        Map<String, IndicatorTally> tally3 = new HashMap<>();
        tally3.put("indicator2", new IndicatorTally(null, 7, "indicator2", null));
        List indicatorTallies = Collections.unmodifiableList(Collections.unmodifiableList(Arrays.asList(tally1, tally2, tally3)));
        long indicator1 = ReportingUtil.getTotalStaticCount(indicatorTallies, "indicator1");
        long indicator2 = ReportingUtil.getTotalStaticCount(indicatorTallies, "indicator2");
        Assert.assertEquals(12, indicator1);
        Assert.assertEquals(7, indicator2);

    }

    @Test
    public void testGetLatestCountBasedOnDate() {
        Map<String, IndicatorTally> tally1 = new HashMap<>();
        tally1.put("indicator2", new IndicatorTally(null, 13, "indicator2", getDateTime(2019, 3, 31, 10, 45, 20)));
        Map<String, IndicatorTally> tally2 = new HashMap<>();
        tally2.put("indicator2", new IndicatorTally(null, 9, "indicator2", getDateTime(2019, 3, 31, 10, 20, 20)));
        List indicatorTallies = Collections.unmodifiableList(Collections.unmodifiableList(Arrays.asList(tally1, tally2)));
        long indicator2 = ReportingUtil.getLatestCountBasedOnDate(indicatorTallies, "indicator2");
        Assert.assertEquals(13, indicator2);
    }

    @Test
    public void testGetIndicatorModelWithStaticTotalAndLatestCount() {
        Map<String, IndicatorTally> tally1 = new HashMap<>();
        tally1.put("indicator1", new IndicatorTally(null, 3, "indicator1", null));
        Map<String, IndicatorTally> tally2 = new HashMap<>();
        tally2.put("indicator1", new IndicatorTally(null, 9, "indicator1", null));
        Map<String, IndicatorTally> tally3 = new HashMap<>();
        tally3.put("indicator2", new IndicatorTally(null, 13, "indicator2", getDateTime(2019, 3, 31, 10, 45, 20)));
        Map<String, IndicatorTally> tally4 = new HashMap<>();
        tally4.put("indicator2", new IndicatorTally(null, 9, "indicator2", getDateTime(2019, 3, 31, 10, 20, 20)));

        List indicatorTallies = Collections.unmodifiableList(Collections.unmodifiableList(Arrays.asList(tally1, tally2, tally3, tally4)));

        //Test get model with static count
        IndicatorModel indicatorModel = ReportingUtil.getIndicatorModel(IndicatorView.CountType.STATIC_COUNT, "indicator1", 182998, indicatorTallies);
        Assert.assertNotNull(indicatorModel);
        Assert.assertEquals(12, indicatorModel.getTotalCount());
        Assert.assertEquals("indicator1", indicatorModel.getIndicatorCode());
        Assert.assertEquals(182998, indicatorModel.getLabelStringResource());

        //Test get model with static count
        IndicatorModel indicatorModel2 = ReportingUtil.getIndicatorModel(IndicatorView.CountType.LATEST_COUNT, "indicator2", 182999, indicatorTallies);
        Assert.assertNotNull(indicatorModel2);
        Assert.assertEquals(13, indicatorModel2.getTotalCount());
        Assert.assertEquals("indicator2", indicatorModel2.getIndicatorCode());
        Assert.assertEquals(182999, indicatorModel2.getLabelStringResource());

        PieChartViewModel pieChartViewModel = ReportingUtil.getPieChartViewModel(indicatorModel, indicatorModel2, null, "Some note");
        Assert.assertNotNull(pieChartViewModel);

    }
}
