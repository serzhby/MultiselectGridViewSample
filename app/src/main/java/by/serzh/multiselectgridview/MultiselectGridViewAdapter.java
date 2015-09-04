package by.serzh.multiselectgridview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashSet;
import java.util.Set;

public abstract class MultiselectGridViewAdapter extends BaseAdapter {
    final Set<Integer> selected = new HashSet<>();

    boolean isInSelectMode;

    public abstract View getView(int position, View convertView, ViewGroup parent, boolean isInSelectMode, boolean isSelected);

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent, isInSelectMode, selected.contains(position));
    }
}
