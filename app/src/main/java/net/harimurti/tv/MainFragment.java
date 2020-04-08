package net.harimurti.tv;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import net.harimurti.tv.adapter.ContentAdapter;
import net.harimurti.tv.data.Channel;

import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class MainFragment extends Fragment {
    private static final String CHANNELS = "channels";
    private ArrayList<Channel> channels;

    public static MainFragment newInstance(ArrayList<Channel> arrayList) {
        Bundle args = new Bundle();
        args.putSerializable(CHANNELS, arrayList);

        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            channels = (ArrayList<Channel>)getArguments().getSerializable(CHANNELS);
        } else {
            channels = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);

        GridView gridView = view.findViewById(R.id.gridview);
        gridView.setAdapter(new ContentAdapter(getContext(), channels));
    }
}
