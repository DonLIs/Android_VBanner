package me.donlis.vbanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BannerAdapter<T> extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {
    public static final int MAX_VALUE = 500;

    private List<T> mList = new ArrayList<>();
    private boolean isCanLoop;
    private VBannerView.OnItemClickListener<T> mOnItemClickListener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(getLayoutId(viewType), parent, false);
        return createViewHolder(inflate, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final int realPosition = BannerUtil.getRealPosition(isCanLoop, position, mList.size());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(mList.get(realPosition), realPosition);
                }
            }
        });
        onBindView(holder.imageView, mList.get(realPosition), realPosition, mList.size());
    }

    @Override
    public int getItemCount() {
        if (isCanLoop && mList.size() > 1) {
            return MAX_VALUE;
        } else {
            return mList.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        int realPosition = BannerUtil.getRealPosition(isCanLoop, position, mList.size());
        return getViewType(realPosition);
    }

    public List<T> getData() {
        return mList;
    }

    public void setData(List<T> list) {
        if (null != list) {
            mList.clear();
            mList.addAll(list);
        }
    }

    public void setCanLoop(boolean canLoop) {
        isCanLoop = canLoop;
    }

    public int getListSize() {
        return mList.size();
    }

    protected int getViewType(int position) {
        return 0;
    }

    protected abstract void onBindView(ImageView imageView, T data, int position, int pageSize);

    protected ViewHolder createViewHolder(View itemView, int viewType){
        return new ViewHolder(itemView);
    }

    protected abstract int getLayoutId(int viewType);

    public VBannerView.OnItemClickListener<T> getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(VBannerView.OnItemClickListener<T> listener) {
        this.mOnItemClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img);
        }

    }

}
