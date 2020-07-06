package com.artifex.mupdf.viewer.gp;


import android.annotation.SuppressLint;
import androidx.recyclerview.widget.RecyclerView;
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
    static class MyViewHolder extends RecyclerView.ViewHolder {
        // data item views
        private RelativeLayout relativeLayout;
        TextView pageNumber;
        ImageView previewImage;

        MyViewHolder(RelativeLayout v) {
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

        return new MyViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {
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
                lastSelectedViewHolder.relativeLayout.setElevation(0);
                holder.pageNumber.setVisibility(VISIBLE);
                holder.relativeLayout.setElevation(20);
                lastSelectedViewHolder = holder;
                selectedIndex = position;
                startAnimation(lastSelectedViewHolder);
            }
        });

        holder.pageNumber.setText(String.valueOf(mDataset[position].getPageNumber()));
        holder.pageNumber.setTextColor(ThemeColor.getInstance().getStrongOppositeThemeColor());
        holder.pageNumber.setTypeface(ThemeFont.getInstance().getMediumItalicFont(mDocumentActivity.getApplicationContext()));

        if (position == selectedIndex) {
            holder.pageNumber.setVisibility(VISIBLE);
            holder.relativeLayout.setElevation(20);
            lastSelectedViewHolder = holder;
        }
        else{
            holder.pageNumber.setVisibility(View.GONE);
            holder.relativeLayout.setElevation(0);
        }

         holder.previewImage.setImageBitmap(mDataset[position].getImage());
         holder.relativeLayout.setTag(position);
    }

    private void startAnimation(final MyViewHolder view ){
        /**Animasyon sürekli tetiklendiği için listener yerine withEndAction kullanıldı. p1597*/
        view.previewImage.animate().alpha(0.6f).setDuration(300).
                translationY((float)view.previewImage.getHeight()/8)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.previewImage.animate().alpha(1).translationY(0);
                    }
                });

        view.pageNumber.animate().setDuration(300).scaleY(0.2f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.pageNumber.animate().scaleY(1);
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