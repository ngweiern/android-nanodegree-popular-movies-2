package com.udacity.popular_movies_2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.udacity.popular_movies_2.database.AppDatabase;
import com.udacity.popular_movies_2.database.Movie;
import com.udacity.popular_movies_2.data.MovieAdapter;
import com.udacity.popular_movies_2.utils.JsonUtils;
import com.udacity.popular_movies_2.utils.NetworkUtils;

import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        MovieAdapter.MovieAdapterOnClickHandler,
        LoaderCallbacks<List<Movie>> {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String POPULAR = "popular";
    private static final String TOP_RATED = "top_rated";

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private TextView mErrorMessageDisplay;
    private TextView mNoInternetMessageDisplay;
    private ProgressBar mLoadingIndicator;
    private static final int MOVIE_LOADER_ID = 0;
    private List<Movie> mMovies;

    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerview);
        mErrorMessageDisplay = findViewById(R.id.error_message_display);
        mNoInternetMessageDisplay = findViewById(R.id.no_internet_connection_message_display);

        GridLayoutManager layoutManager
                = new GridLayoutManager(this, 2, RecyclerView.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mMovieAdapter = new MovieAdapter(this);
        mRecyclerView.setAdapter(mMovieAdapter);
        mLoadingIndicator = findViewById(R.id.pb_loading_indicator);

        int loaderId = MOVIE_LOADER_ID;
        LoaderCallbacks<List<Movie>> callback = MainActivity.this;
        Bundle bundleForLoader = new Bundle();
        bundleForLoader.putString("path", POPULAR);

        if (!isOnline()) {
            showNoInternetConnectionMessage();
        } else {
            getSupportLoaderManager().initLoader(loaderId, bundleForLoader, callback);
        }
    }

    @SuppressLint("StaticFieldLeak")
    @NonNull
    @Override
    public Loader<List<Movie>> onCreateLoader(int id, @Nullable final Bundle bundle) {
        final String path = bundle.getString("path");

        return new AsyncTaskLoader<List<Movie>>(this) {
            List<Movie> mMoviesData = null;

            @Override
            protected void onStartLoading() {
                if (mMoviesData != null) {
                    deliverResult(mMoviesData);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Nullable
            @Override
            public List<Movie> loadInBackground() {
                URL moviesRequestUrl = NetworkUtils.buildUrl(path);

                try {
                    String jsonMoviesResponse = NetworkUtils
                            .getResponseFromHttpUrl(moviesRequestUrl);

                    List<Movie> listMoviesData = JsonUtils
                            .getMoviesFromJson(jsonMoviesResponse);

                    return listMoviesData;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public void deliverResult(List<Movie> data) {
                mMoviesData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Movie>> loader, List<Movie> movies) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mMovies = movies;
        mMovieAdapter.setMovieData(movies);
        if (!isOnline()) {
            showNoInternetConnectionMessage();
        } else if (null == movies) {
            showErrorMessage();
        } else {
            showMoviesDataView();
        }
        getSupportLoaderManager().destroyLoader(MOVIE_LOADER_ID);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Movie>> loader) {
    }

    private void invalidateData() {
        mMovieAdapter.setMovieData(null);
    }

    @Override
    public void onClick(Movie movie) {
        Context context = this;
        Class destinationClass = MovieActivity.class;
        Intent intentToStartMovieActivity = new Intent(context, destinationClass);
        intentToStartMovieActivity.putExtra("movie", movie);
        startActivity(intentToStartMovieActivity);
    }

    private void showMoviesDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mNoInternetMessageDisplay.setVisibility(View.INVISIBLE);
    }

    private void showErrorMessage() {
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
        mNoInternetMessageDisplay.setVisibility(View.INVISIBLE);
    }

    private void showNoInternetConnectionMessage() {
        mNoInternetMessageDisplay.setVisibility(View.VISIBLE);
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundleForLoader = new Bundle();

        switch (item.getItemId()) {
            case R.id.action_sort_rated:
                bundleForLoader.putString("path", TOP_RATED);
                getSupportLoaderManager().initLoader(MOVIE_LOADER_ID, bundleForLoader, MainActivity.this);
                mMovieAdapter.setMovieData(mMovies);
                return true;
            case R.id.action_sort_popular:
                bundleForLoader.putString("path", POPULAR);
                getSupportLoaderManager().initLoader(MOVIE_LOADER_ID, bundleForLoader, MainActivity.this);
                mMovieAdapter.setMovieData(mMovies);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
