package com.github.andlyticsproject.legacy;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.andlyticsproject.BaseChartListAdapter;
import com.github.andlyticsproject.ChartSwitcher;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

@SuppressWarnings("deprecation")
public abstract class BaseChartActivity extends BaseDetailsActivity implements
		ViewSwitcherListener, ChartSwitcher {

	static final String SELECTED_CHART_POISTION = "selected_chart_position";

	private ChartGalleryAdapter chartGalleryAdapter;
	private ChartGallery chartGallery;
	private ListView dataList;
	private TextView timeframeText;
	protected String timetext;
	private Timeframe currentTimeFrame;
	private ViewSwitcher3D listViewSwitcher;
	private ViewGroup dataframe;
	private ViewGroup chartframe;

	BaseChartListAdapter myAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basechart);
		List<View> extras;

		extras = getExtraFullViews();
		if (extras != null) {
			ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.base_chart_viewswitcher_config);
			for (View v : extras)
				vs.addView(v);
		}

		currentTimeFrame = Preferences.getChartTimeframe(this);

		listViewSwitcher = new ViewSwitcher3D(
				(ViewGroup) findViewById(R.id.base_chart_bottom_frame));
		listViewSwitcher.setListener(this);

		chartGallery = (ChartGallery) findViewById(R.id.base_chart_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				chartGallery.setIgnoreLayoutCalls(true);

				if (view.getTag() != null) {
					int pageColumn[] = (int[]) view.getTag();
					myAdapter.setCurrentChart(pageColumn[0], pageColumn[1]);
					updateChartHeadline();
					myAdapter.notifyDataSetChanged();
					onChartSelected(pageColumn[0], pageColumn[1]);

				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		dataList = (ListView) findViewById(R.id.base_chart_list);
		timeframeText = (TextView) findViewById(R.id.base_chart_timeframe);
		dataframe = (ViewGroup) findViewById(R.id.base_chart_datacontainer);
		chartframe = (ViewGroup) findViewById(R.id.base_chart_chartframe);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(SELECTED_CHART_POISTION, chartGallery.getSelectedItemPosition());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		int chartIndex = state.getInt(SELECTED_CHART_POISTION);
		chartGallery.setSelection(chartIndex);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.charts_menu, menu);
		MenuItem activeTimeFrame = null;
		switch (currentTimeFrame) {
		case LAST_SEVEN_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe7);
			break;
		case LAST_THIRTY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe30);
			break;
		case LAST_NINETY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe90);
			break;
		case UNLIMITED:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeUnlimited);
			break;
		case MONTH_TO_DATE:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeMonthToDate);
			break;
		}
		activeTimeFrame.setChecked(true);

		if (isRefreshing()) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}

		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 *
	 * @param item
	 * The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemChartsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			executeLoadData(currentTimeFrame);
			return true;
		case R.id.itemChartsmenuToggle:
			toggleChartData(item);
			return true;
		case R.id.itemChartsmenuTimeframe7:
			currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe30:
			currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe90:
			currentTimeFrame = Timeframe.LAST_NINETY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_NINETY_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeUnlimited:
			currentTimeFrame = Timeframe.UNLIMITED;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.UNLIMITED, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeMonthToDate:
			currentTimeFrame = Timeframe.MONTH_TO_DATE;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.MONTH_TO_DATE, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	/**
	 * Toggles visibility of chart/data UI parts.
	 */
	private void toggleChartData(MenuItem item) {
		if (View.VISIBLE == chartframe.getVisibility()) {
			chartframe.setVisibility(View.GONE);
			dataframe.setVisibility(View.VISIBLE);
			item.setIcon(this.getResources().getDrawable(R.drawable.icon_graph));
		} else {
			chartframe.setVisibility(View.VISIBLE);
			dataframe.setVisibility(View.GONE);
			item.setIcon(this.getResources().getDrawable(R.drawable.icon_data));
		}
	}

	/**
	 * Called when chart is selected
	 *
	 * @param page
	 * @param column
	 */
	protected void onChartSelected(int page, int column) {
	}

	@Override
	public void setCurrentChart(int page, int column) {
		int pos = 0;
		for (View view : chartGalleryAdapter.getViews()) {
			int pageColumn[] = (int[]) view.getTag();
			if (page == pageColumn[0] && column == pageColumn[1]) {
				chartGallery.setSelection(pos, false);
				return;
			}
			pos++;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	protected abstract void notifyChangedDataformat();

	protected List<View> getExtraFullViews() {
		return null;
	}

	protected abstract void executeLoadData(Timeframe currentTimeFrame);

	protected final void setAdapter(BaseChartListAdapter adapter) {
		myAdapter = adapter;
		dataList.setAdapter(adapter);
	}

	private final void updateTitleTextSwitcher(String string) {
		getSupportActionBar().setTitle(string);
	}

	@Override
	protected void onResume() {
		super.onResume();
		chartGallery.setIgnoreLayoutCalls(false);
	}

	@Override
	public void onViewChanged(boolean frontsideVisible) {
		chartGallery.setIgnoreLayoutCalls(true);

	}

	@Override
	public void onRender() {
		chartGallery.invalidate();

	}

	protected final void setChartIgnoreCallLayouts(boolean ignoreLayoutCalls) {
		chartGallery.setIgnoreLayoutCalls(ignoreLayoutCalls);
	}

	public void updateCharts(List<?> statsForApp) {
		Chart chart = new Chart();
		int page = myAdapter.getCurrentPage();
		int column = myAdapter.getCurrentColumn();

		int position = -1;
		List<View> charts = new ArrayList<View>();

		int pos = 0;
		for (int i = 0; i < myAdapter.getNumPages(); i++)
			for (int j = 1; j < myAdapter.getNumCharts(i); j++) {
				int pageColumn[] = new int[3];
				View chartView = myAdapter.buildChart(this, chart, statsForApp, i, j);
				/*
				 * if(chartView==null) {
				 * Log.i(LOG_TAG,"Ignoring chart p="+i+" c="+j+"for class="
				 * +this.getClass().toString()); continue; }
				 */
				Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT);
				chartView.setLayoutParams(params);
				pageColumn[0] = i;
				pageColumn[1] = j;
				pageColumn[2] = myAdapter.getNumCharts(i);
				if (i == page && j == column)
					position = pos;
				pos++;
				chartView.setTag(pageColumn);
				charts.add(chartView);
			}
		chartGallery.setIgnoreLayoutCalls(false);
		chartGalleryAdapter.setViews(charts);
		if (position >= 0)
			chartGallery.setSelection(position);
		chartGalleryAdapter.notifyDataSetChanged();
		chartGallery.invalidate();
	}

	protected final void updateChartHeadline() {

		String subHeadlineText = "";
		String title = myAdapter.getCurrentChartTitle();
		String ret = myAdapter.getCurrentSubHeadLine();
		if (ret != null)
			subHeadlineText = ret;

		updateTitleTextSwitcher(title);

		if (Preferences.getShowChartHint(this)) {
			timeframeText.setText(Html.fromHtml(getChartHint()));
		} else {
			if (timetext != null) {
				timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
			}
		}

	}

	public Timeframe getCurrentTimeFrame() {
		return currentTimeFrame;
	}

	public ViewSwitcher3D getListViewSwitcher() {
		return listViewSwitcher;
	}

	protected abstract String getChartHint();

	public void setAllowChangePageSliding(boolean allowChangePageSliding) {
		chartGallery.setAllowChangePageSliding(allowChangePageSliding);
	}

}
