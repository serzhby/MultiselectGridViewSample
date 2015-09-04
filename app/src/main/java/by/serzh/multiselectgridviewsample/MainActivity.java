package by.serzh.multiselectgridviewsample;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;

import by.serzh.multiselectgridview.MultiselectGridView;
import by.serzh.multiselectgridview.MultiselectGridViewAdapter;

public class MainActivity extends AppCompatActivity {

    private MultiselectGridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (MultiselectGridView) findViewById(R.id.grid_view);
        gridView.setAdapter(new GridAdapter());
    }

    private class GridAdapter extends MultiselectGridViewAdapter {

        @Override
        public int getCount() {
            return 80;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent, boolean isInSelectMode, boolean isSelected) {
            TextView textView = convertView == null
                    ? new TextView(MainActivity.this)
                    : (TextView) convertView;

            textView.setText(String.valueOf(position));

            textView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            if(isSelected) {
                textView.setBackgroundColor(Color.RED);
            } else {
                textView.setBackgroundColor(isInSelectMode ? Color.BLUE : Color.TRANSPARENT);
            }
            textView.setTag(position);
            return textView;
        }
    }

    private int dpToPx(float dp) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    @Override
    public void onBackPressed() {
        if(gridView.isInSelectMode()) {
            Collection<Integer> selectedPositions = gridView.getSelectedItems();
            StringBuilder sb = new StringBuilder();
            for(Integer selectedPosition : selectedPositions) {
                sb.append(selectedPosition).append(", ");
            }
            Toast.makeText(this, sb, Toast.LENGTH_LONG).show();
            gridView.finishSelect();
        } else {
            super.onBackPressed();
        }
    }
}
