package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;


import static com.example.xyzreader.ui.ArticleListActivity.currentPosition;

/**
 * Shared Element Transition setup
 * <p>
 * 4. Set a unique transition name for each thumbnail
 * 5. startActivity setup
 * 6. start postponed enter transition when image resource is ready
 * -> {@link ArticleDetailActivity}
 */
public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {
    private Cursor mCursor;

    private Activity mActivity;

    private static final String TAG = "ArticleListAdapter";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private static SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private static GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private final ViewHolderListener viewHolderListener;
    private final RequestManager requestManager;

    public ArticleListAdapter(Activity activity, Cursor cursor) {
        mCursor = cursor;
        mActivity = activity;
        this.requestManager = Glide.with(mActivity);
        this.viewHolderListener = new ViewHolderListenerImpl(mActivity);
    }

    /**
     * A listener that is attached to all ViewHolders to handle image loading events and clicks.
     */
    private interface ViewHolderListener {
        void onLoadCompleted(int adapterPosition);
        void onItemClicked(View view, int adapterPosition, long id);
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(view, requestManager, viewHolderListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.onBind(mCursor, mActivity);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {

        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        private final RequestManager requestManager;
        private final ViewHolderListener viewHolderListener;

        public ViewHolder(View view, RequestManager requestManager, ViewHolderListener viewHolderListener) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            this.viewHolderListener = viewHolderListener;
            this.requestManager = requestManager;
            view.setOnClickListener(this);
        }

        /**
         * Binds this view holder to the given adapter position.
         * <p>
         * The binding will load the image into the image view, as well as set its transition name for
         * later.
         */
        void onBind(Cursor cursor, Activity activity) {
            String title = cursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(title);
            Date publishedDate = parsePublishedDate(cursor);
            CharSequence subtitle;
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                subtitle = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + cursor.getString(ArticleLoader.Query.AUTHOR));
            } else {
                subtitle = Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + cursor.getString(ArticleLoader.Query.AUTHOR));
            }
            subtitleView.setText(subtitle);
            String imageUrl = cursor.getString(ArticleLoader.Query.THUMB_URL);
            thumbnailView.setAspectRatio(cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            int adapterPosition = getAdapterPosition();
            setImage(adapterPosition, imageUrl);
            // 4
            // Set the string value of the image resource as the unique transition name for the view.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                thumbnailView.setTransitionName(imageUrl);
                Log.i("ArticleListAdapter", "Setting transition name: " + imageUrl);
            }
        }

        private Date parsePublishedDate(Cursor cursor) {
            try {
                String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        void setImage(final int adapterPosition, final String imageUrl) {
            // Load the image with Glide to prevent OOM error when the image drawables are very large.
            Log.i("ArticleListAdapter", "loading image: " + imageUrl);
            requestManager
                    .load(imageUrl)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            viewHolderListener.onLoadCompleted(adapterPosition);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            viewHolderListener.onLoadCompleted(adapterPosition);
                            return false;
                        }

                    })
                    .into(thumbnailView);
        }

        @Override
        public void onClick(View view) {
            // Let the listener start the ImagePagerFragment.
            viewHolderListener.onItemClicked(view, getAdapterPosition(), getItemId());
        }
    }

    /**
     * Default {@link ViewHolderListener} implementation.
     */
    private static class ViewHolderListenerImpl implements ViewHolderListener {

        private Activity activity;
        private AtomicBoolean enterTransitionStarted;

        ViewHolderListenerImpl(Activity activity) {
            this.activity = activity;
            this.enterTransitionStarted = new AtomicBoolean();
        }

        @Override
        public void onLoadCompleted(int position) {
            // Call startPostponedEnterTransition only when the 'selected' image loading is completed.
            if (currentPosition != position) {
                return;
            }
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }

            // 6
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.startPostponedEnterTransition();
            }
        }

        /**
         * Handles a view click by setting the current position to the given {@code position} and
         * starting a {@link  ArticleDetailActivity} which displays the image at the position.
         *
         * @param view            the clicked {@link ImageView} (the shared element view will be re-mapped at the
         *                        GridFragment's SharedElementCallback)
         * @param adapterPosition the selected view position
         */
        @Override
        public void onItemClicked(View view, int adapterPosition, long id) {
            // Update the position.
            currentPosition = adapterPosition;

            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(id));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // 5
                // create the transition animation
                // get the common element for the transition in this activity
                final View transitionImageView = view.findViewById(R.id.thumbnail);
                String transitionNameImage = transitionImageView.getTransitionName();
                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(
                                activity,
                                Pair.create(transitionImageView, transitionNameImage)
                        );
                // start the new activity
                activity.startActivity(intent, options.toBundle());
            } else {
                activity.startActivity(intent);
            }
        }

    }

}
