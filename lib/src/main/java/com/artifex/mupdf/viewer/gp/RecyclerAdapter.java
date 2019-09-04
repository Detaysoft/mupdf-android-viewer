package com.artifex.mupdf.viewer.gp;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artifex.mupdf.viewer.DocumentActivity;
import com.artifex.mupdf.viewer.R;
import com.artifex.mupdf.viewer.gp.models.PagePreview;
import com.artifex.mupdf.viewer.gp.util.ThemeColor;
import com.artifex.mupdf.viewer.gp.util.ThemeFont;


import static android.view.View.VISIBLE;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private PagePreview[] mDataset;

    // PDF related variables
    private DocumentActivity mDocumentActivity;
    private int selectedIndex = 0;

    private MyViewHolder lastSelectedViewHolder;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // data item views
        public RelativeLayout relativeLayout;
        public TextView pageNumber;
        public ImageView previewImage;

        public MyViewHolder(RelativeLayout v) {
            super(v);
            pageNumber = v.findViewById(R.id.pageNumber);
            previewImage = v.findViewById(R.id.previewImage);
            relativeLayout = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(PagePreview[] myDataset, DocumentActivity documentActivity) {
        mDataset = myDataset;
        mDocumentActivity = documentActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_preview, parent, false);

        v.setBackgroundColor(ThemeColor.getInstance().getThemeColor());

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        // display clicked page and change layout for both selected and last selected page
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if clicked the same page again
                if (position == selectedIndex)
                    return;

                mDocumentActivity.jumpToPageAtIndex(position);
                lastSelectedViewHolder.pageNumber.setVisibility(View.GONE);
                //viewVisibleAnimator(holder.pageNumber);
                holder.pageNumber.setVisibility(VISIBLE);
                lastSelectedViewHolder = holder;
                selectedIndex = position;
            }
        });

        holder.pageNumber.setText(String.valueOf(mDataset[position].getPageNumber()));
        holder.pageNumber.setTextColor(ThemeColor.getInstance().getStrongOppositeThemeColor());
        holder.pageNumber.setTypeface(ThemeFont.getInstance().getMediumItalicFont(mDocumentActivity.getApplicationContext()));

        if (position == selectedIndex) {
            holder.pageNumber.setVisibility(VISIBLE);
            //viewVisibleAnimator(holder.pageNumber);
            lastSelectedViewHolder = holder;
        }
        else
         holder.pageNumber.setVisibility(View.GONE);

         holder.previewImage.setImageBitmap(mDataset[position].getImage());
         holder.relativeLayout.setTag(position);
    }

    //animasyon iki kere tıklanma sorununa sebep olduğu için kaldırıldı.
    private void viewVisibleAnimator(final View view) {
        view.animate()
                .alpha(1f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(VISIBLE);
                    }
                });
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }


    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
}