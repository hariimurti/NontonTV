package net.harimurti.tv.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.harimurti.tv.PlayerActivity;
import net.harimurti.tv.R;
import net.harimurti.tv.data.Channel;

import java.util.ArrayList;

@SuppressWarnings("all")
public class ContentAdapter extends ArrayAdapter<Channel> {
    private ArrayList<Channel> dataSet;
    private int display;

    public ContentAdapter(Context context, ArrayList<Channel> dataSet) {
        super(context, R.layout.fragment_content, dataSet);
        this.dataSet = dataSet;

        Point screen = new Point();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(screen);
        display = screen.x < screen.y ? screen.x : screen.y;
    }

    private class ContentHolder {
        RelativeLayout layout;
        TextView textView;

        public ContentHolder(View v) {
            layout = v.findViewById(R.id.layout);
            textView = v.findViewById(R.id.textview);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        final Context context = getContext();
        Channel data = getItem(i);
        ContentHolder viewHolder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.fragment_content, parent, false);
            viewHolder = new ContentHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ContentHolder) view.getTag();
        }

        if (display < 720) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            viewHolder.layout.getLayoutParams().height =
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, dm);
        }
        viewHolder.textView.setText(data.name);
        viewHolder.layout.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("channel_url", data.stream_url);
            context.startActivity(intent);
        });
        viewHolder.layout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) return;

                GridView gridView = (GridView)parent;

                int columns = gridView.getNumColumns();
                int first = gridView.getFirstVisiblePosition();
                int last = gridView.getLastVisiblePosition();

                if (first <= i && i <= (first+columns+1)) {
                    int position = first - columns;
                    if (position < 0) position = 0;

                    gridView.smoothScrollToPosition(position);
                }
                if ((last-columns+1) <= i && i <= last) {
                    int position = last + columns;
                    if (position > last) position = last;

                    gridView.smoothScrollToPosition(last + columns);
                }
            }
        });

        return view;
    }
}
