package com.pengrad.raspkursk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import rx.Observable;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int REQUEST_CODE_CHOOSE_STATIONS = 120;

    private YandexRaspService mYandexRaspService;
    private SearchTrainsRecyclerAdapter mTrainsAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Station mStationFrom, mStationTo;
    private EditText mEditTextFrom;
    private EditText mEditTextTo;

    private StationManager mStationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.button_switch_stations).setOnClickListener(v -> doSwitchStations());
        findViewById(R.id.button_find).setOnClickListener(v -> doSearch());

        mEditTextFrom = (EditText) findViewById(R.id.edittext_from);
        mEditTextTo = (EditText) findViewById(R.id.edittext_to);
        mEditTextFrom.setOnClickListener(v -> doChooseStations());
        mEditTextTo.setOnClickListener(v -> doChooseStations());

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this::doSearch);

        mTrainsAdapter = new SearchTrainsRecyclerAdapter();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mTrainsAdapter);

        mYandexRaspService = ServiceBuilder.yandexRaspService();
        mStationManager = new StationManager(getResources());
        mStationFrom = mStationManager.getDefaultFromStation();
        mStationTo = mStationManager.getDefaultToStation();
        updateStationTitles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private void doChooseStations() {
        Intent intent = ChooseStationsActivity.getIntent(this, mStationFrom, mStationTo);
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_STATIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CHOOSE_STATIONS && resultCode == RESULT_OK) {
            String stationFrom = data.getStringExtra(ChooseStationsActivity.EXTRA_STATION_FROM);
            String stationTo = data.getStringExtra(ChooseStationsActivity.EXTRA_STATION_TO);

            mStationFrom = mStationManager.getStationByCode(stationFrom);
            mStationTo = mStationManager.getStationByCode(stationTo);
            updateStationTitles();
            doSearch();
        }
    }

    private void doSwitchStations() {
        Station _from = mStationFrom;
        mStationFrom = mStationTo;
        mStationTo = _from;
        updateStationTitles();
        doSearch();
    }

    private void updateStationTitles() {
        mEditTextFrom.setText(mStationFrom.title);
        mEditTextTo.setText(mStationTo.title);
    }

    private void doSearch() {
        mSwipeRefreshLayout.setRefreshing(true);
        AppObservable.bindActivity(this, searchRequest())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSearchResponse);
    }

    private void onSearchResponse(SearchResponse searchResponse) {
        mSwipeRefreshLayout.setRefreshing(false);
        mTrainsAdapter.setData(searchResponse);
    }

    private Observable<SearchResponse> searchRequest() {
        return mYandexRaspService.search(mStationFrom.code, mStationTo.code, null);
    }
}
