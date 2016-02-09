package mvasari.popularmoviesapp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MovieFragment extends Fragment {

    ImageListAdapter mMoviesAdapter;
    String[] moviePosters  = {
            "http://i.imgur.com/rFLNqWI.jpg",
            "http://i.imgur.com/C9pBVt7.jpg"
    };

    public MovieFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        FetchMovieClass movieTask = new FetchMovieClass();
        movieTask.execute();

//        ArrayList<String> lst = new ArrayList<String>(Arrays.asList(moviePosters));

        mMoviesAdapter = new ImageListAdapter(rootView.getContext(), new ArrayList<String>(Arrays.asList(moviePosters)));
        GridView gridView = (GridView) rootView.findViewById(R.id.gridview_movies);
        gridView.setAdapter(mMoviesAdapter);

//        Picasso.with(rootView.getContext()).load("http://i.imgur.com/DvpvklR.png").into(imageView);

        return rootView;
    }


    private void updateMovies()
    {
        FetchMovieClass movieTask = new FetchMovieClass();
        movieTask.execute();
    }

    public class ImageListAdapter extends ArrayAdapter {
        private Context context;
        private LayoutInflater inflater;

        private List<String> imageUrls;

        public ImageListAdapter(Context context, List<String> imageUrls) {
            super(context, R.layout.grid_item_movie, imageUrls);

            this.context = context;
            this.imageUrls = imageUrls;

            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = inflater.inflate(R.layout.grid_item_movie, parent, false);
            }

            Picasso
                    .with(context)
                    .load(imageUrls.get(position))
                    .fit() // will explain later
                    .into((ImageView) convertView);

            return convertView;
        }

        public void updateImageList(List<String> newImagelist) {
            if(this.imageUrls != null){
                this.imageUrls.clear();
                this.imageUrls.addAll(newImagelist);
            }else{
                this.imageUrls = newImagelist;
            }
            notifyDataSetChanged();
        }
    }

    public class FetchMovieClass extends AsyncTask<String,Void,String[]> {

        private final String LOG_TAG = FetchMovieClass.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length != 0 ) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String moviesJsonStr;
            String sortBy= "popularity.desc";

            try {
                // Construct the URL for the TheMovieDB query
                // Possible parameters are avaiable at TheMovieDB API page, at
                // https://api.themoviedb.org/

                final String MOVIES_BASE_URL = "https://api.themoviedb.org/3";
                final String DISCOVER = "discover";
                final String MOVIE = "movie";
                final String SORT_BY = "sort_by";
                final String API_KEY = "api_key";

                Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        .appendPath(DISCOVER)
                        .appendPath(MOVIE)
                        .appendQueryParameter(SORT_BY,sortBy)
                        .appendQueryParameter(API_KEY,getResources().getString(R.string.API_KEY)).build();

                URL url = new URL(builtUri.toString());
//                Log.e("teste", url.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) return null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the data, there's no point in attemping to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(moviesJsonStr);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                e.printStackTrace();
            }

            return null;


        }

        private String[] getMovieDataFromJson(String moviesJsonStr)
                throws JSONException {

            final String TMD_RESULTS = "results";
            final String TMD_TITLE = "title";
            final String TMD_POSTER_PATH = "poster_path";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMD_RESULTS);

            String[] resultStrs = new String[moviesArray.length()];
            moviePosters = new String[moviesArray.length()];
            for(int i = 0; i < moviesArray.length(); i++) {
                JSONObject movieData = moviesArray.getJSONObject(i);
                String title = movieData.getString(TMD_POSTER_PATH);
                resultStrs[i] = title;
                Log.e("titulos", title);
                moviePosters[i] = "http://image.tmdb.org/t/p/w185" + title;
            }

            return moviePosters;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mMoviesAdapter.updateImageList(new ArrayList<String>(Arrays.asList(result)));
                /* mMoviesAdapter.clear();
                for (String movieStr : result) mMoviesAdapter.add(movieStr);
                */
            }
        }
    }

}
