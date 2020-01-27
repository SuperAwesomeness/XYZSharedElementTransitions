package com.example.xyzreader.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.Article;


//adapter class
// https://guides.codepath.com/android/Heterogenous-Layouts-inside-RecyclerView#overview

/**
 * 10. Set unique transition name
 * 11. Start postponed Enter Transition when image resource is ready
 */
public class ArticleDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Article articleElements;

    private final int IMAGE = 0, TITLE = 1, BODY = 2;

    private final ViewHolderListener viewHolderListener;
    private final RequestManager requestManager;

    private Activity context;

    ArticleDetailAdapter(Activity activity) {
        this.context = activity;
        this.requestManager = Glide.with(context);
        this.viewHolderListener = new ViewHolderListenerImpl(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        switch (viewType) {
            case IMAGE:
                View v1 = inflater.inflate(R.layout.list_item_detail_image, viewGroup, false);
                viewHolder = new ArticleImageViewHolder(v1);
                break;
            case TITLE:
                View v2 = inflater.inflate(R.layout.list_item_detail_title, viewGroup, false);
                viewHolder = new ArticleTitleViewHolder(v2);
                break;
            default: // body text
                View v = inflater.inflate(R.layout.list_item_detail_body, viewGroup, false);
                viewHolder = new ArticleBodyViewHolder(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case IMAGE:
                ArticleImageViewHolder vhImage = (ArticleImageViewHolder) viewHolder;
                configureImageViewHolder(vhImage);
                break;
            case TITLE:
                ArticleTitleViewHolder vhTitle = (ArticleTitleViewHolder) viewHolder;
                configureTitleViewHolder(vhTitle);
                break;
            default:
                ArticleBodyViewHolder vhBody = (ArticleBodyViewHolder) viewHolder;
                configureTextBodyViewHolder(vhBody, position);
                break;
        }
    }

    /**
     * A listener that is attached to all ViewHolders to handle image loading events and clicks.
     */
    private interface ViewHolderListener {
        void onLoadCompleted();
    }

    /**
     * Default {@link ViewHolderListener} implementation.
     */
    private static class ViewHolderListenerImpl implements ViewHolderListener {

        private Activity activity;

        ViewHolderListenerImpl(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onLoadCompleted() {
            // 11
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.startPostponedEnterTransition();
            }
        }

    }

    private void configureTextBodyViewHolder(ArticleBodyViewHolder vhBody, int position) {
        String[] bodyPart = articleElements.getArticleBody();
        String singleTextElement = bodyPart[position - 2];
        vhBody.articleBodyTextView.setText(Html.fromHtml(singleTextElement));
    }

    private void configureTitleViewHolder(ArticleTitleViewHolder vhTitle) {
        String title = articleElements.getTitle();
        vhTitle.articleTitleTextView.setText(Html.fromHtml(title));
        CharSequence byline = Html.fromHtml(articleElements.getByline());
        vhTitle.articleBylineTextView.setText(byline);
        int mMutedColor = 0xFF333333;
        vhTitle.metaBar.setBackgroundColor(mMutedColor);
    }

    private void configureImageViewHolder(ArticleImageViewHolder vhImage) {
        final DynamicHeightNetworkImageView mPhotoView = vhImage.articleImage;
        // Just like we do when binding views at the grid, we set the transition name to be the string
        // value of the image res.
        final String imageUrl = articleElements.getImagePath();
        // 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName(imageUrl);
            Log.i("ArticleDetailAdapter", "Setting transition name: " + imageUrl);
        }

        float aspectRatio = articleElements.getAspectRatio();
        mPhotoView.setAspectRatio(aspectRatio);
        Log.i("ArticleDetailAdapter", "setting aspect ratio: " + aspectRatio);

        // Load the image with Glide to prevent OOM error when the image drawables are very large.
        requestManager
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted();
                        Log.i("ArticleDetailAdapter", "Image url: " + imageUrl);
                        Log.i("ArticleDetailAdapter", "onResourceReady...");
                        return false;
                    }

                })
                .into(mPhotoView);
    }

    //Returns the view type of the item at position for the purposes of view recycling.
    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case IMAGE:
            case TITLE:
                return position;
        }
        return BODY;
    }

    @Override
    public int getItemCount() {
        if (articleElements != null) {
            Log.i("Adapter", "item count: " + articleElements.getArticleBody().length + 2);
            return articleElements.getArticleBody().length + 2; // text body + image + title (title+byline)
        }
        return 0;
    }

    void setArticleData(Article articleElements) {
        this.articleElements = articleElements;
        notifyDataSetChanged();
    }

    class ArticleImageViewHolder extends RecyclerView.ViewHolder {

        final DynamicHeightNetworkImageView articleImage;

        ArticleImageViewHolder(View view) {
            super(view);
            articleImage = view.findViewById(R.id.article_detail_image);
        }

    }

    class ArticleTitleViewHolder extends RecyclerView.ViewHolder {

        final TextView articleTitleTextView;
        final TextView articleBylineTextView;
        final LinearLayout metaBar;

        ArticleTitleViewHolder(View view) {
            super(view);
            articleTitleTextView = (TextView) view.findViewById(R.id.article_detail_title);
            articleBylineTextView = (TextView) view.findViewById(R.id.article_detail_byline);
            metaBar = view.findViewById(R.id.meta_bar);
            articleBylineTextView.setMovementMethod(new LinkMovementMethod());
        }

    }

    class ArticleBodyViewHolder extends RecyclerView.ViewHolder {

        final TextView articleBodyTextView;

        ArticleBodyViewHolder(View view) {
            super(view);
            articleBodyTextView = (TextView) view.findViewById(R.id.article_body_text);
        }

    }
}
